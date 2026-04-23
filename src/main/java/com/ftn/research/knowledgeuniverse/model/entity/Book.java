package com.ftn.research.knowledgeuniverse.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

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

    @Column(name = "title", nullable = false)
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

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "book_genre",
            joinColumns = @JoinColumn(name = "book_isbn"),
            inverseJoinColumns = @JoinColumn(name = "genre_id"))
    private Set<Genre> genres = new HashSet<>();


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Book book = (Book) o;
        return isbn.equals(book.isbn) && title.equals(book.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isbn, title);
    }
}
