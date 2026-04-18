package com.ftn.research.knowledgeuniverse.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "book")
//      TODO migrate Book storage from RDB to Neo4j
public class Book {

//    TODO explore the idea of moving from UUID to Long
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String isbn;

    @Column(name = "author")
    private String author;

    @Column(name = "title")
    private String title;

//    TODO uncomment - majority text language store inside DataBase
    @Column(name = "lang")
    private String language;

//    @Column(name = "content_native")
    @Transient
    private String contentInNativeLang;

    @Column(name = "server_filename")
    private String serverFilename;

    @Column(name = "mime_type")
    private String mimeType;
}
