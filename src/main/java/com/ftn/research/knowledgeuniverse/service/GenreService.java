package com.ftn.research.knowledgeuniverse.service;

import com.ftn.research.knowledgeuniverse.model.entity.Genre;
import com.ftn.research.knowledgeuniverse.model.entity.User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public interface GenreService {
    Optional<Genre> findById(Long genreId);

    List<Genre> findAll();

    List<Genre> findFavoriteGenresForUser(User user);
}
