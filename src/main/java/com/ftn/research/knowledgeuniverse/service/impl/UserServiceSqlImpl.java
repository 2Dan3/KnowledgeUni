package com.ftn.research.knowledgeuniverse.service.impl;

import com.ftn.research.knowledgeuniverse.model.dto.UserCreationDTO;
import com.ftn.research.knowledgeuniverse.model.entity.Genre;
import com.ftn.research.knowledgeuniverse.model.entity.User;
import com.ftn.research.knowledgeuniverse.repository.entity.UserRepository;
import com.ftn.research.knowledgeuniverse.service.GenreService;
import com.ftn.research.knowledgeuniverse.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

@Service
public class UserServiceSqlImpl implements UserService {

    private UserRepository userRepository;
//    private PasswordEncoder passwordEncoder;
    private GenreService genreService;

    @Autowired
    public UserServiceSqlImpl(/*@Lazy PasswordEncoder passwordEncoder,*/ UserRepository userRepository, GenreService genreService) {
//        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.genreService = genreService;
    }

    @Override
    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }

    @Override
    public boolean isEmailTaken(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public User createUser(UserCreationDTO userCreationDTO) {

        User u = new User();
        u.setEmail(userCreationDTO.getEmail());
        u.setPassword(userCreationDTO.getPassword());
//        u.setPassword(passwordEncoder.encode(userCreationDTO.getPassword()));

        for (Long genreId : userCreationDTO.getFavoriteGenresIDs()) {

            Optional<Genre> genre = genreService.findById(genreId);

            genre.ifPresent(value -> u.getGenres().add(value));
        }

        return userRepository.save(u);
    }

    @Override
    public Optional<User> findByCredentials(String email, String password) {
        return userRepository.findByCredentials(email, password);
    }


}
