package com.ftn.research.knowledgeuniverse.service.impl;

import ai.djl.inference.Predictor;
import ai.djl.translate.TranslateException;
import ai.djl.repository.zoo.ZooModel;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingService {

    private final ZooModel<String, float[]> model;

    public EmbeddingService(ZooModel<String, float[]> model) {
        this.model = model;
    }

    public float[] getEmbedding(String text) throws TranslateException {
        try (Predictor<String, float[]> predictor = model.newPredictor()) {
            return Nd4j.create(predictor.predict(text)).toFloatVector();
        }
    }

    public double cosineSimilarity(INDArray vectorA, INDArray vectorB) {
        double dotProduct = vectorA.mul(vectorB).sumNumber().doubleValue();
        double magnitudeA = vectorA.norm2Number().doubleValue();
        double magnitudeB = vectorB.norm2Number().doubleValue();
        return dotProduct / (magnitudeA * magnitudeB);
    }

//    todo add this helper
//    private String getContentForEmbedding(ChunkDocument doc) {
//    if (doc.getContentEn() != null) return doc.getContentEn();
//    if (doc.getContentSr() != null) return doc.getContentSr();
//    if (doc.getContentDe() != null) return doc.getContentDe();
//    if (doc.getContentFr() != null) return doc.getContentFr();
//    if (doc.getContentRu() != null) return doc.getContentRu();
//    if (doc.getContentEs() != null) return doc.getContentEs();
//    if (doc.getContentIt() != null) return doc.getContentIt();
//    if (doc.getContentPt() != null) return doc.getContentPt();
//    if (doc.getContentUk() != null) return doc.getContentUk();
//
//    return "";
//}
}