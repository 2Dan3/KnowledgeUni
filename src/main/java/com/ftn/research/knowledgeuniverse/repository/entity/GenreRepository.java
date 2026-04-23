package com.ftn.research.knowledgeuniverse.repository.entity;

import com.ftn.research.knowledgeuniverse.model.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Long> {

    List<Genre> findAllByUser_id(Long userId);
}
