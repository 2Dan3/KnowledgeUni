package com.ftn.research.knowledgeuniverse.config;

import ai.djl.ModelException;
import ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class EmbeddingConfig {

    private static final String HUGGINGFACE_MODEL = "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2";

    @Bean(destroyMethod = "close")
    public ZooModel<String, float[]> embeddingModel() throws IOException, ModelException {
        // Use DJL HuggingFace model zoo
        Criteria<String, float[]> criteria = Criteria.builder()
                .setTypes(String.class, float[].class)
                .optModelUrls("djl://ai.djl.huggingface.pytorch/" + HUGGINGFACE_MODEL)
                .optTranslatorFactory(new TextEmbeddingTranslatorFactory())
                .optProgress(new ProgressBar())
                .build();

        return criteria.loadModel();
    }
}