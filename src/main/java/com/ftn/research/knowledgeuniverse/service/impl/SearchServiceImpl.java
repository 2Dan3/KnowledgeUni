package com.ftn.research.knowledgeuniverse.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import com.ftn.research.knowledgeuniverse.model.index.BookIndex;
import com.ftn.research.knowledgeuniverse.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.language.detect.LanguageDetector;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService {

    private final ElasticsearchClient client;
    private final EmbeddingService embeddingService;
    private final LanguageDetector languageDetector;

    @Override
    public Page<BookIndex> simpleSearch(List<String> keywords, Pageable pageable, boolean isKNN) {

        String queryText = String.join(" ", keywords);

        // language detection
        String queryLang = languageDetector.detect(queryText).getLanguage().toUpperCase();
        if (queryLang.equals("HR")) queryLang = "SR";

        String boostedField = "content_" + queryLang.toLowerCase() + "^5";

        // embedding
        float[] embedding;
        try {
            embedding = embeddingService.getEmbedding(queryText);
        } catch (Exception e) {
            log.error("Embedding failed", e);
            return Page.empty();
        }

        List<Float> vector = new ArrayList<>(embedding.length);
        for (float f : embedding) vector.add(f);

        try {

            int candidateSize = 50;

            // ph 1.1: BM25
            SearchResponse<BookIndex> bm25Response = client.search(s -> s
                            .index("book_index")
                            .size(candidateSize)
                            .query(q -> q
                                    .multiMatch(m -> m
                                            .query(queryText)
                                            .fields(
                                                    "title^3",
                                                    boostedField,
                                                    "content_en^2",
                                                    "content_sr^2",
                                                    "content_de^2",
                                                    "content_fr^2",
                                                    "content_ru^2",
                                                    "content_es^2",
                                                    "content_it^2",
                                                    "content_pt^2",
                                                    "content_uk^2"
                                            )
                                    )
                            )
                            .trackTotalHits(t -> t.enabled(false))
                    , BookIndex.class);

            // ph 1.2: KNN
            SearchResponse<BookIndex> knnResponse = client.search(s -> s
                            .index("book_index")
                            .size(candidateSize)
                            .knn(k -> k
                                    .field("vector")
                                    .queryVector(vector)
                                    .k(candidateSize)
                                    .numCandidates(200)
                            )
                    , BookIndex.class);

            // merging of IDs (BM25 & KNN)
            Set<String> idSet = new LinkedHashSet<>();

            bm25Response.hits().hits().forEach(hit -> {
                if (hit.id() != null) idSet.add(hit.id());
            });

            knnResponse.hits().hits().forEach(hit -> {
                if (hit.id() != null) idSet.add(hit.id());
            });

            if (idSet.isEmpty()) {
                return Page.empty();
            }

            List<String> ids = new ArrayList<>(idSet);

            // ph 2: hybrid manual re-ranking
            // BM25 contribution (from ES scoring) + normalizing 0–1 range (to prevent bm25 dominance on long docs)
            // + Vector similarity
            // + Hybrid weighting (tunable)
            SearchResponse<BookIndex> response = client.search(s -> s
                            .index("book_index")
                            .from((int) pageable.getOffset())
                            .size(pageable.getPageSize())

                            .query(q -> q
                                    .scriptScore(ss -> ss
                                            .query(q2 -> q2
                                                    .ids(i -> i.values(ids))
                                            )
                                            .script(sc -> sc
                                                    .source("""
                                                        double bm25 = Math.log(1 + _score);
                                                        bm25 = bm25 / (1 + bm25);
                                                        
                                                        double vector = cosineSimilarity(params.query_vector, 'vector');
                                                        vector = (vector + 1.0) / 2.0;
                                                        
                                                        double bm25Weight = 0.4;
                                                        double vectorWeight = 0.6;
                                                
                                                        return (bm25Weight * bm25) + (vectorWeight * vector);
                                                    """)
                                                    .params("query_vector", JsonData.of(vector))
                                            )
                                    )
                            )

                            .sort(so -> so.score(sc -> sc.order(co.elastic.clients.elasticsearch._types.SortOrder.Desc)))
                    , BookIndex.class);

            // collecting of results
            List<BookIndex> results = new ArrayList<>();
            List<String> resultIds = new ArrayList<>();

            response.hits().hits().forEach(hit -> {
                if (hit.source() != null) {
                    results.add(hit.source());
                }
                if (hit.id() != null) {
                    resultIds.add(hit.id());
                }
            });

            // highlighting
            SearchResponse<BookIndex> highlightResponse = client.search(s -> s
                            .index("book_index")
                            .size(resultIds.size())
                            .query(q -> q.ids(i -> i.values(resultIds)))
                            .highlight(h -> h
                                    .fields("content", f -> f.fragmentSize(150).numberOfFragments(5))
                                    .fields("content_en", f -> f.fragmentSize(150).numberOfFragments(5))
                                    .fields("content_sr", f -> f.fragmentSize(150).numberOfFragments(5))
                                    .fields("content_de", f -> f.fragmentSize(150).numberOfFragments(5))
                                    .fields("content_fr", f -> f.fragmentSize(150).numberOfFragments(5))
                                    .fields("content_ru", f -> f.fragmentSize(150).numberOfFragments(5))
                                    .fields("content_es", f -> f.fragmentSize(150).numberOfFragments(5))
                                    .fields("content_it", f -> f.fragmentSize(150).numberOfFragments(5))
                                    .fields("content_pt", f -> f.fragmentSize(150).numberOfFragments(5))
                                    .fields("content_uk", f -> f.fragmentSize(150).numberOfFragments(5))
                            )
                    , BookIndex.class);

            Map<String, BookIndex> resultMap = new HashMap<>();

            for (var hit : response.hits().hits()) {
                if (hit.source() != null) {
                    resultMap.put(hit.id(), hit.source());
                }
            }

            for (var hit : highlightResponse.hits().hits()) {

                BookIndex doc = resultMap.get(hit.id());
                if (doc == null) continue;

                if (hit.highlight() != null) {

                    List<String> allFragments = new ArrayList<>();

                    hit.highlight().forEach((field, fragments) -> {
                        allFragments.addAll(fragments);
                    });

                    if (!allFragments.isEmpty()) {
                        doc.setContent(String.join("...<br/>...", allFragments));
                    }
                }
            }

            // total (candidate-based, RRF-like)
            long total = idSet.size();

            return new PageImpl<>(results, pageable, total);

        } catch (Exception e) {
            log.error("Search failed", e);
            return Page.empty();
        }
    }
}