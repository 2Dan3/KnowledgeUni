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
@Setting(settingPath = "/configuration/serbian-analyzer-config.json")
public class BookIndex {

    @Id
    private String id;

//    @Field(type = FieldType.Text, store = true, name = "author")
//    private String author;

    @Field(type = FieldType.Text, store = true, name = "title")
    private String title;

    @Field(type = FieldType.Text, store = true, name = "content_sr", analyzer = "serbian_simple", searchAnalyzer = "serbian_simple")
    private String contentSr;

    @Field(type = FieldType.Text, store = true, name = "content_en", analyzer = "english", searchAnalyzer = "english")
    private String contentEn;

    @Field(type = FieldType.Text, store = true, name = "content_ru", analyzer = "russian", searchAnalyzer = "russian")
    private String contentRu;

    @Field(type = FieldType.Text, store = true, name = "content_uk", analyzer = "ukrainian", searchAnalyzer = "ukrainian")
    private String contentUk;

    @Field(type = FieldType.Text, store = true, name = "content_it", analyzer = "italian", searchAnalyzer = "italian")
    private String contentIt;

    @Field(type = FieldType.Text, store = true, name = "content_es", analyzer = "spanish", searchAnalyzer = "spanish")
    private String contentEs;

    @Field(type = FieldType.Text, store = true, name = "content_fr", analyzer = "french", searchAnalyzer = "french")
    private String contentFr;

    @Field(type = FieldType.Text, store = true, name = "content_de", analyzer = "german", searchAnalyzer = "german")
    private String contentDe;

    @Field(type = FieldType.Text, store = true, name = "content_pt", analyzer = "portuguese", searchAnalyzer = "portuguese")
    private String contentPt;

    @Field(type = FieldType.Text, store = true, name = "server_filename", index = false)
    private String serverFilename;

    @Field(type = FieldType.Keyword, store = true, name = "database_isbn")
    private String databaseISBN;

    @Field(type = FieldType.Dense_Vector, dims = 384, similarity = "cosine")
    private float[] vectorizedContent;
}
