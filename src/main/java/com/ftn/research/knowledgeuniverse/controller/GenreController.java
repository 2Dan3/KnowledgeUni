package com.ftn.research.knowledgeuniverse.controller;

import com.ftn.research.knowledgeuniverse.model.dto.GenreResponseDTO;
import com.ftn.research.knowledgeuniverse.model.entity.Genre;
import com.ftn.research.knowledgeuniverse.service.GenreService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/genres")
@RequiredArgsConstructor
public class GenreController {

    @Autowired
    private GenreService genreService;

    @GetMapping
    public ResponseEntity<List<GenreResponseDTO>> getAllGenres() {

        List<Genre> foundGenres = genreService.findAll();

        List<GenreResponseDTO> genreDTOs = foundGenres.stream().map(GenreResponseDTO::new).toList();

        return ResponseEntity.ok().body(genreDTOs);
    }
}
