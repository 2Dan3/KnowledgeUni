package com.ftn.research.knowledgeuniverse.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "book")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String isbn;

    @Column(name = "author")
    private String author;

    @Column(name = "title")
    private String title;

//    @Column(name = "content_native")
    @Transient
    private String contentInNativeLang;

    @Column(name = "server_filename")
    private String serverFilename;

    @Column(name = "mime_type")
    private String mimeType;
}
