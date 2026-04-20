package com.ftn.research.knowledgeuniverse.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import com.ftn.research.knowledgeuniverse.model.index.BookIndex;
import com.ftn.research.knowledgeuniverse.service.BulkIndexingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
@Slf4j
public class BulkIndexingServiceImpl implements BulkIndexingService {

//    private final BlockingQueue<BookIndex> queue = new LinkedBlockingQueue<>(5000);
    private final BlockingQueue<BookIndex> queue = new LinkedBlockingQueue<>(5000);

    private final ElasticsearchClient client;
    private final EmbeddingService embeddingService;

    public BulkIndexingServiceImpl(ElasticsearchClient client,
                                   EmbeddingService embeddingService) {
        this.client = client;
        this.embeddingService = embeddingService;

        startConsumers();
    }

    @Override
    public void submit(BookIndex doc) {
        try {
            queue.put(doc);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void startConsumers() {

        int workers = 4;

        for (int i = 0; i < workers; i++) {
            new Thread(() -> {

//                List<BookIndex> batch = new ArrayList<>(500);
                List<BookIndex> batch = new ArrayList<>(5000);

                while (true) {
                    try {
                        BookIndex doc = queue.take();
                        batch.add(doc);

                        if (batch.size() >= 500) {
                            flush(batch);
                            batch.clear();
                        }

                    } catch (Exception e) {
                        log.error("Worker failed", e);
                    }
                }
            }).start();
        }
    }

    private void flush(List<BookIndex> batch) {
        try {

            List<BulkOperation> ops = new ArrayList<>();

            for (BookIndex doc : batch) {

                String textForEmbedding =
                        "Title: " + doc.getTitle() + "\n" +
                        "Content: " + doc.getContent();

                doc.setVector(embeddingService.getEmbedding(textForEmbedding));

                Map<String, Object> json = new HashMap<>();

                json.put("book_id", doc.getBookId());
                json.put("title", doc.getTitle());
                json.put("chunk_index", doc.getChunkIndex());
                json.put("language", doc.getChunkLanguage());

                json.put("content_sr", doc.getChunkLanguage().equals("SR") ? doc.getContent() : null);
                json.put("content_en", doc.getChunkLanguage().equals("EN") ? doc.getContent() : null);
                json.put("content_de", doc.getChunkLanguage().equals("DE") ? doc.getContent() : null);
                json.put("content_fr", doc.getChunkLanguage().equals("FR") ? doc.getContent() : null);
                json.put("content_ru", doc.getChunkLanguage().equals("RU") ? doc.getContent() : null);
                json.put("content_es", doc.getChunkLanguage().equals("ES") ? doc.getContent() : null);
                json.put("content_it", doc.getChunkLanguage().equals("IT") ? doc.getContent() : null);
                json.put("content_pt", doc.getChunkLanguage().equals("PT") ? doc.getContent() : null);
                json.put("content_uk", doc.getChunkLanguage().equals("UK") ? doc.getContent() : null);

                json.put("content", doc.getContent());
                json.put("vector", doc.getVector());

                ops.add(BulkOperation.of(b -> b
                        .index(idx -> idx
                                .index("book_index")
                                .id(doc.getBookId() + "_" + doc.getChunkIndex())
                                .document(json)
                        )
                ));
            }

            BulkRequest request = BulkRequest.of(b -> b.operations(ops));

            BulkResponse response = client.bulk(request);

            if (response.errors()) {
                log.error("Bulk indexing had errors");
            }

        } catch (Exception e) {
            log.error("Bulk flush failed", e);
        }
    }
}