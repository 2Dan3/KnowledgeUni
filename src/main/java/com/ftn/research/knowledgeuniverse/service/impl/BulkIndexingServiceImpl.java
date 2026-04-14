package com.ftn.research.knowledgeuniverse.service.impl;

@Service
@Slf4j
public class BulkIndexingService {

    private final BlockingQueue<ChunkDocument> queue = new LinkedBlockingQueue<>(5000);

    private final BulkProcessor bulkProcessor;
    private final EmbeddingService embeddingService;

    public BulkIndexingService(ElasticsearchClient client,
                               EmbeddingService embeddingService) {

        this.embeddingService = embeddingService;

        this.bulkProcessor = BulkProcessor.builder(
                        (request, listener) ->
                                client.bulkAsync(request, listener),
                        new BulkProcessor.Listener() {

                            public void beforeBulk(long id, BulkRequest req) {}

                            public void afterBulk(long id, BulkRequest req, BulkResponse res) {
                                if (res.errors()) {
                                    log.error("Bulk errors!");
                                }
                            }

                            public void afterBulk(long id, BulkRequest req, Throwable t) {
                                log.error("Bulk failed", t);
                            }
                        })
                .setBulkActions(500)
                .setConcurrentRequests(2)
                .build();
//        TODO finetune setbulkactions and setconcurrentrequests numbers according to performance needs

        startConsumers();
    }

    // 🔥 Producer
    public void submit(ChunkDocument doc) {
        try {
            queue.put(doc); // backpressure safe
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // 🔥 Consumers (embedding + bulk)
    private void startConsumers() {
        int workers = 4;

        for (int i = 0; i < workers; i++) {
            new Thread(() -> {
                while (true) {
                    try {
                        ChunkDocument doc = queue.take();

                        // ✅ async embedding (blocking here per worker)
                        doc.setVector(embeddingService.getEmbedding(doc.getContent()));

                        IndexRequest request = new IndexRequest("book_chunks")
//                                TODO OLD id building
//                                .id(UUID.randomUUID().toString())
                                .id(doc.getBookId() + "_" + doc.getChunkIndex())
                                .source(Map.of(
                                        "book_id", doc.getBookId(),
                                        "title", doc.getTitle(),
                                        "content", doc.getContent(),
                                        "chunk_index", doc.getChunkIndex(),
                                        "language", doc.getLanguage(),
                                        "database_isbn", doc.getDatabaseISBN(),
                                        "vector", doc.getVector()
                                ));

                        bulkProcessor.add(request);
//                        todo into the line above, put following
//                        ChunkDocument doc = queue.take();
//
                        //// 1️⃣ Embed
                        //doc.setVector(embeddingService.getEmbedding(getContentForEmbedding(doc)));
                        //
                        //// 2️⃣ Build JSON (🔥 THIS IS WHERE YOUR CODE GOES)
                        //Map<String, Object> json = new HashMap<>();
                        //
                        //json.put("book_id", doc.getBookId());
                        //json.put("title", doc.getTitle());
                        //json.put("chunk_index", doc.getChunkIndex());
                        //json.put("database_isbn", doc.getDatabaseISBN());
                        //
                        //// ✅ language fields (ONLY ONE WILL BE NON-NULL)
                        //json.put("content_sr", doc.getContentSr());
                        //json.put("content_en", doc.getContentEn());
                        //json.put("content_de", doc.getContentDe());
                        //json.put("content_fr", doc.getContentFr());
                        //json.put("content_ru", doc.getContentRu());
                        //json.put("content_es", doc.getContentEs());
                        //json.put("content_it", doc.getContentIt());
                        //json.put("content_pt", doc.getContentPt());
                        //json.put("content_uk", doc.getContentUk());
                        //
                        //// ✅ vector
                        //json.put("vector", doc.getVector());
                        //
                        //// 3️⃣ Create request
                        //IndexRequest request = new IndexRequest("book_chunks")
                        //        .id(UUID.randomUUID().toString())
                        //        .source(json);
                        //
                        //// 4️⃣ Send to bulk
                        //bulkProcessor.add(request);

                    } catch (Exception e) {
                        log.error("Worker failed", e);
                    }
                }
            }).start();
        }
    }
}