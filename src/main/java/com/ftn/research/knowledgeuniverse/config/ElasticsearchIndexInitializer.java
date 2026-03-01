package com.ftn.research.knowledgeuniverse.config;

import com.ftn.research.knowledgeuniverse.model.index.BookIndex;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
//import org.springframework.data.elasticsearch.core.index.IndexOperations;

@Configuration
@RequiredArgsConstructor
public class ElasticsearchIndexInitializer {

    private final ElasticsearchOperations operations;

    @Bean
    public ApplicationRunner initializeBookIndex() {
        return args -> {

            IndexOperations indexOps = operations.indexOps(BookIndex.class);

            if (!indexOps.exists()) {

                // 1️⃣ Create index with settings from @Setting
                indexOps.create();

                // 2️⃣ Create mapping from annotations (multi-language + dense_vector)
                indexOps.putMapping(indexOps.createMapping());

                System.out.println("\n\nBookIndex created successfully.\n\n");
            }
        };
    }
}