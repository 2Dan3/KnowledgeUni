package com.ftn.research.knowledgeuniverse.repository.entity;

import com.ftn.research.knowledgeuniverse.model.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookRepository extends JpaRepository<Book, String> {
}
