package com.ftn.research.knowledgeuniverse.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
//import co.elastic.clients.elasticsearch._types.rank.RrfRank;
import com.ftn.research.knowledgeuniverse.model.index.BookIndex;
import com.ftn.research.knowledgeuniverse.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.language.detect.LanguageDetector;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        // 🔥 detect query language
        String queryLang = languageDetector.detect(queryText).getLanguage().toUpperCase();
        if (queryLang.equals("HR")) queryLang = "SR";

        String boostedField = "content_" + queryLang.toLowerCase() + "^5";

        // 🔥 embedding
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
            SearchRequest request = SearchRequest.of(s -> s
                    .index("book_index")
                    .from((int) pageable.getOffset())
                    .size(pageable.getPageSize())

                    // 🔥 BM25 multilingual
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

                    // 🔥 vector
                    .knn(k -> k
                            .field("vector")
                            .queryVector(vector)
                            .k(50)
                            .numCandidates(200)
                    )

                    // 🔥 TRUE HYBRID (RRF)
                    .rank(r -> r
                            .rrf(rrf -> rrf
                                    .rankWindowSize(50L) // 40-100 lower = unstable rank, higher = noise
                                    .rankConstant(60L) // 50-80 lower = more vector influence, higher = more bm25 influence
                            )
                    )
            );

            SearchResponse<BookIndex> response =
                    client.search(request, BookIndex.class);

            List<BookIndex> results = new ArrayList<>();
            List<String> ids = new ArrayList<>();

            response.hits().hits().forEach(hit -> {
                if (hit.source() != null) {
                    results.add(hit.source());
                }
                if (hit.id() != null) {
                    ids.add(hit.id());
                }
            });

            SearchRequest highlightRequest = SearchRequest.of(s -> s
                    .index("book_index")
                    .size(ids.size())

                    // 🔥 restrict to retrieved docs
                    .query(q -> q
                            .ids(i -> i.values(ids))
                    )

                    // 🔥 SAME BM25 query (important for relevance of snippets)
                    .highlight(h -> h
                            .fields("content", f -> f
                                    .fragmentSize(150)
                                    .numberOfFragments(5)
                            )
                            .fields("content_en", f -> f
                                    .fragmentSize(150)
                                    .numberOfFragments(5)
                            )
                            .fields("content_sr", f -> f.fragmentSize(150).numberOfFragments(5))
                            .fields("content_de", f -> f.fragmentSize(150).numberOfFragments(5))
                            .fields("content_fr", f -> f.fragmentSize(150).numberOfFragments(5))
                            .fields("content_ru", f -> f.fragmentSize(150).numberOfFragments(5))
                            .fields("content_es", f -> f.fragmentSize(150).numberOfFragments(5))
                            .fields("content_it", f -> f.fragmentSize(150).numberOfFragments(5))
                            .fields("content_pt", f -> f.fragmentSize(150).numberOfFragments(5))
                            .fields("content_uk", f -> f.fragmentSize(150).numberOfFragments(5))
//                            TODO uncomment to tune results
//                            .numberOfFragments(3)
//                            .requireFieldMatch(false)
//                            .preTags("<em>")
//                            .postTags("</em>")
                    )
            );

            SearchResponse<BookIndex> highlightResponse =
                    client.search(highlightRequest, BookIndex.class);

            Map<String, BookIndex> resultMap = new HashMap<>();

            // original results
            for (var hit : response.hits().hits()) {
                if (hit.source() != null) {
                    resultMap.put(hit.id(), hit.source());
                }
            }

            // 🔥 apply highlights
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
                    // ⚠️ fallback: keep original chunk content if no highlight
                }
            }

//            TODO check this line
            long total = response.hits().total() != null
                    ? response.hits().total().value()
                    : results.size();

            return new PageImpl<>(results, pageable, total);

        } catch (Exception e) {
            log.error("Search failed", e);
            return Page.empty();
        }
    }
}