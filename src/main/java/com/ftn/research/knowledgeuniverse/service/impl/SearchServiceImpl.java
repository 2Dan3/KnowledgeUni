package com.ftn.research.knowledgeuniverse.service.impl;

import ai.djl.translate.TranslateException;
import co.elastic.clients.elasticsearch._types.KnnQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import com.ftn.research.knowledgeuniverse.exceptionhandling.exception.MalformedQueryException;
import com.ftn.research.knowledgeuniverse.model.index.BookIndex;
import com.ftn.research.knowledgeuniverse.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.common.unit.Fuzziness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService {

    private final ElasticsearchOperations elasticsearchTemplate;

    private final EmbeddingService embeddingService;

//    @Override
//    public Page<BookIndex> simpleSearch(List<String> keywords, Pageable pageable, boolean isKNN) {
//
//        if (isKNN) {
//            try {
//                return searchByVector(embeddingService.getEmbedding(String.join(" ", keywords)));
//            } catch (TranslateException e) {
//                log.error("Vectorization failed", e);
//                return Page.empty();
//            }
//        }
//
//        // 1️⃣ Build query
//        NativeQuery query = new NativeQueryBuilder()
//                .withQuery(buildSimpleSearchQuery(keywords))
//                .withPageable(pageable)
//                .build();
//
//        // 2️⃣ Attach highlighting
//        query.setHighlightQuery(new HighlightQuery(buildHighlight(), BookIndex.class));
//
//        return runQuery(query);
//    }

    private Query buildHybridQuery(List<String> tokens, List<Float> vector) {

        BoolQuery.Builder bool = new BoolQuery.Builder();

        String queryText = String.join(" ", tokens);

        // 🔥 BM25 (stronger)
        bool.should(s -> s.multiMatch(m -> m
                .query(queryText)
                .fields(
                        "content_sr^2.0",
                        "content_en^2.0",
                        "content_de^2.0",
                        "content_fr^2.0",
                        "content_ru^2.0",
                        "content_es^2.0",
                        "content_it^2.0",
                        "content_pt^2.0",
                        "content_uk^2.0",
                        "title^3.0"
                )
        ));

        // 🔥 Vector (weaker)
        bool.should(s -> s.knn(k -> k
                .field("vector")
                .queryVector(vector)
                .k(20)
                .numCandidates(100)
                .boost(0.7f)
        ));

        return bool.build()._toQuery();
    }

    @Override
    public Page<ChunkDocument> simpleSearch(List<String> keywords, Pageable pageable, boolean isKNN) {

        String queryText = String.join(" ", keywords);

        // 1️⃣ Get embedding
        float[] embedding;
        try {
            embedding = embeddingService.getEmbedding(queryText);
        } catch (Exception e) {
            log.error("Embedding failed", e);
            return Page.empty();
        }

        // 2️⃣ Convert to List<Float>
        List<Float> vector = new ArrayList<>(embedding.length);
        for (float f : embedding) {
            vector.add(f);
        }

        // 3️⃣ 🔥 USE HYBRID QUERY HERE (THIS IS THE ONLY CHANGE)
        NativeQuery query = new NativeQueryBuilder()
                .withQuery(buildHybridQuery(keywords, vector))
                .withPageable(pageable)
                .build();

        // 4️⃣ Keep highlight EXACTLY as before
        query.setHighlightQuery(new HighlightQuery(buildHighlight(), ChunkDocument.class));

        // 5️⃣ Execute
        return runQuery(query);


//        todo where to put this if i replaced buildSimple w/ hybridSearch
//        float[] embedding = embeddingService.getEmbedding(queryText);
//
//        List<Float> vector = new ArrayList<>();
//        for (float f : embedding) vector.add(f);
//
//        NativeQuery query = new NativeQueryBuilder()
//                .withQuery(buildHybridQuery(tokens, vector))
//                .withPageable(pageable)
//                .build();
    }

    @Override
    public Page<BookIndex> advancedSearch(List<String> expression, Pageable pageable) {

        if (expression.size() != 3) {
            throw new MalformedQueryException("Search query malformed.");
        }

        String operation = expression.get(1);
        expression.remove(1);

        NativeQuery query = new NativeQueryBuilder()
                .withQuery(buildAdvancedSearchQuery(expression, operation))
                .withPageable(pageable)
                .build();

        query.setHighlightQuery(new HighlightQuery(buildHighlight(), BookIndex.class));

        return runQuery(query);
    }

    public Page<BookIndex> searchByVector(float[] queryVector) {

        // ✅ Convert float[] to List<Float>
        List<Float> floatList = new ArrayList<>(queryVector.length);
        for (float f : queryVector) {
            floatList.add(f); // autobox to Float
        }

        // 1️⃣ Build KNN query
        var knnQuery = new KnnQuery.Builder()
                .field("vectorizedContent")
                .queryVector(floatList)
                .numCandidates(100)
                .k(10)
                .boost(10.0f)
                .build();

        // 2️⃣ Build NativeQuery
        NativeQuery searchQuery = NativeQuery.builder()
                .withKnnQuery(knnQuery)
                .withMaxResults(5)
                .build();

        // 3️⃣ Attach highlighting for content fields
        searchQuery.setHighlightQuery(new HighlightQuery(buildHighlight(), BookIndex.class));

        // 4️⃣ Run query & apply highlights
        return runQuery(searchQuery);
    }

    // =========================
    // Build highlight for content_sr & content_en
    // =========================
//    private Highlight buildHighlight() {
//        return new Highlight(
//                HighlightParameters.builder()
//                        .withPreTags("<em>")
//                        .withPostTags("</em>")
//                        .withFragmentSize(100)
//                        .withNumberOfFragments(10)
//                        .build(),
//                List.of(
//                        new HighlightField("content_sr"),
//                        new HighlightField("content_en"),
//                        new HighlightField("content_de"),
//                        new HighlightField("content_ru"),
//                        new HighlightField("content_fr"),
//                        new HighlightField("content_es"),
//                        new HighlightField("content_it"),
//                        new HighlightField("content_pt"),
//                        new HighlightField("content_uk")
//                )
//        );
//    }

    private Highlight buildHighlight() {
        return new Highlight(
                HighlightParameters.builder()
                        .withPreTags("<em>")
                        .withPostTags("</em>")
                        .withFragmentSize(150)
                        .withNumberOfFragments(5)
                        .build(),
                List.of(new HighlightField("content"))
        );
    }

    // =========================
    // Simple search query
    // =========================
    private co.elastic.clients.elasticsearch._types.query_dsl.Query buildSimpleSearchQuery(List<String> tokens) {

        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        for (String token : tokens) {
            // Fuzzy match on title
            boolBuilder.should(s -> s.match(m -> m.field("title")
                    .fuzziness(Fuzziness.ONE.asString())
                    .query(token)));

            // Match on content_sr with boost
            boolBuilder.should(s -> s.match(m -> m.field("content_sr")
                    .query(token)
                    .boost(0.5f)));

            // Match on content_en
            boolBuilder.should(s -> s.match(m -> m.field("content_en")
                    .query(token)));

            // Match on content_en
            boolBuilder.should(s -> s.match(m -> m.field("content_de")
                    .query(token)));

            // Match on content_en
            boolBuilder.should(s -> s.match(m -> m.field("content_fr")
                    .query(token)));

            // Match on content_en
            boolBuilder.should(s -> s.match(m -> m.field("content_ru")
                    .query(token)));

            // Match on content_en
            boolBuilder.should(s -> s.match(m -> m.field("content_es")
                    .query(token)));

            // Match on content_en
            boolBuilder.should(s -> s.match(m -> m.field("content_it")
                    .query(token)));

            // Match on content_en
            boolBuilder.should(s -> s.match(m -> m.field("content_pt")
                    .query(token)));

            // Match on content_en
            boolBuilder.should(s -> s.match(m -> m.field("content_uk")
                    .query(token)));
        }

        return boolBuilder.build()._toQuery();
    }

    // =========================
    // Advanced search query
    // =========================
    private co.elastic.clients.elasticsearch._types.query_dsl.Query buildAdvancedSearchQuery(List<String> operands, String operation) {

        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        var field1 = operands.get(0).split(":")[0];
        var value1 = operands.get(0).split(":")[1];
        var field2 = operands.get(1).split(":")[0];
        var value2 = operands.get(1).split(":")[1];

        switch (operation) {
            case "AND" -> {
                boolBuilder.must(m -> m.match(mm -> mm.field(field1)
                        .fuzziness(Fuzziness.ONE.asString())
                        .query(value1)));
                boolBuilder.must(m -> m.match(mm -> mm.field(field2)
                        .query(value2)));
            }
            case "OR" -> {
                boolBuilder.should(m -> m.match(mm -> mm.field(field1)
                        .fuzziness(Fuzziness.ONE.asString())
                        .query(value1)));
                boolBuilder.should(m -> m.match(mm -> mm.field(field2)
                        .query(value2)));
            }
            case "NOT" -> {
                boolBuilder.must(m -> m.match(mm -> mm.field(field1)
                        .fuzziness(Fuzziness.ONE.asString())
                        .query(value1)));
                boolBuilder.mustNot(m -> m.match(mm -> mm.field(field2)
                        .query(value2)));
            }
        }

        return boolBuilder.build()._toQuery();
    }

    // =========================
    // Execute query + apply highlight
    // =========================
//    private Page<BookIndex> runQuery(NativeQuery searchQuery) {
//
//        SearchHits<BookIndex> searchHits =
//                elasticsearchTemplate.search(searchQuery, BookIndex.class);
//
//        List<BookIndex> results = new ArrayList<>();
//
//        for (SearchHit<BookIndex> hit : searchHits) {
////            System.out.println(hit.getHighlightFields());
//
//            // Apply highlight if available
//            Map<String, List<String>> highlight = hit.getHighlightFields();
//            BookIndex book = hit.getContent();
//
//            highlight.forEach((field, fragments) -> {
//                String combined = String.join("...<br/><br/>...", fragments);
//
//                switch (field) {
//                    case "contentSr" -> book.setContentSr(combined);
//                    case "contentEn" -> book.setContentEn(combined);
//                    case "contentDe" -> book.setContentDe(combined);
//                    case "contentRu" -> book.setContentRu(combined);
//                    case "contentFr" -> book.setContentFr(combined);
//                    case "contentEs" -> book.setContentEs(combined);
//                    case "contentIt" -> book.setContentIt(combined);
//                    case "contentPt" -> book.setContentPt(combined);
//                    case "contentUk" -> book.setContentUk(combined);
//                }
//            });
//
//            results.add(book);
//        }
////        System.out.println(searchQuery.getQuery());
//        return new PageImpl<>(results, searchQuery.getPageable(), searchHits.getTotalHits());
//    }

    private Page<ChunkDocument> runQuery(NativeQuery query) {

        SearchHits<ChunkDocument> hits =
                elasticsearchTemplate.search(query, ChunkDocument.class);

        List<ChunkDocument> results = new ArrayList<>();

        for (SearchHit<ChunkDocument> hit : hits) {

            ChunkDocument doc = hit.getContent();

            Map<String, List<String>> highlight = hit.getHighlightFields();

            if (highlight.containsKey("content")) {
                doc.setContent(String.join("...<br/>...", highlight.get("content")));
            }

            results.add(doc);
        }

        return new PageImpl<>(results, query.getPageable(), hits.getTotalHits());
    }
}