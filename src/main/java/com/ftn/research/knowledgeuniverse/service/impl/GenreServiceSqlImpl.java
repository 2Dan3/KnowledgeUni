package com.ftn.research.knowledgeuniverse.service.impl;

import com.ftn.research.knowledgeuniverse.model.entity.Genre;
import com.ftn.research.knowledgeuniverse.model.entity.User;
import com.ftn.research.knowledgeuniverse.repository.entity.GenreRepository;
import com.ftn.research.knowledgeuniverse.service.GenreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GenreServiceSqlImpl implements GenreService {

    @Autowired
    private GenreRepository genreRepository;

    @Override
    public Optional<Genre> findById(Long genreId) {
        return genreRepository.findById(genreId);
    }

    @Override
    public List<Genre> findAll() {
        return genreRepository.findAll();
    }

    @Override
    public List<Genre> findFavoriteGenresForUser(User user) {
        return genreRepository.findAllByUser_id(user.getId());
    }
}
