package com.ftn.research.knowledgeuniverse.model.index;

import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

@Data
@NoArgsConstructor
@Document(indexName = "book_index")
@Setting(settingPath = "/configuration/multilang-analyzer-config.json")
public class BookIndex {

    @Id
    private String id;

    @Field(type = FieldType.Keyword, name = "book_id")
    private String bookId;

    @Field(type = FieldType.Text, store = true, name = "title", analyzer = "icu_analyzer")
    private String title;

    // 🔥 KEEP ALL LANGUAGE FIELDS (your analyzers are GOLD)
    @Field(type = FieldType.Text, name = "content_sr", analyzer = "serbian_simple", searchAnalyzer = "serbian_simple")
    private String contentSr;

    @Field(type = FieldType.Text, name = "content_en", analyzer = "english", searchAnalyzer = "english")
    private String contentEn;

    @Field(type = FieldType.Text, name = "content_de", analyzer = "german", searchAnalyzer = "german")
    private String contentDe;

    @Field(type = FieldType.Text, name = "content_fr", analyzer = "french", searchAnalyzer = "french")
    private String contentFr;

    @Field(type = FieldType.Text, name = "content_ru", analyzer = "russian_indexing", searchAnalyzer = "russian_searching")
    private String contentRu;

    @Field(type = FieldType.Text, name = "content_es", analyzer = "spanish", searchAnalyzer = "spanish")
    private String contentEs;

    @Field(type = FieldType.Text, name = "content_it", analyzer = "italian", searchAnalyzer = "italian")
    private String contentIt;

    @Field(type = FieldType.Text, name = "content_pt", analyzer = "portuguese", searchAnalyzer = "portuguese")
    private String contentPt;

    @Field(type = FieldType.Text, name = "content_uk", analyzer = "ukrainian", searchAnalyzer = "ukrainian")
    private String contentUk;

    // 🔥 RAG metadata
    @Field(type = FieldType.Integer, name = "chunk_index")
    private int chunkIndex;

//    TODO remove entirely - no need inside ES, true DataBase ID is already linking it to its original Book entity
//    @Field(type = FieldType.Keyword, name = "database_isbn")
//    private String databaseISBN;

    // 🔥 VECTOR
    @Field(type = FieldType.Dense_Vector, dims = 384, similarity = "cosine", index = true)
    private float[] vector;

    @Field(type = FieldType.Text, name = "content", analyzer = "icu_analyzer")
    private String content;
}