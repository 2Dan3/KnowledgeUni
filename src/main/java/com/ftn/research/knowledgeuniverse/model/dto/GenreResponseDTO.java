package com.ftn.research.knowledgeuniverse.model.dto;

import com.ftn.research.knowledgeuniverse.model.entity.Genre;

public class GenreResponseDTO {
    private Long id;
    private String name;

    public GenreResponseDTO(Genre genre) {
        this.id = genre.getId();
        this.name = genre.getName();
    }
}
