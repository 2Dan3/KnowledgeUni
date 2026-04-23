package com.ftn.research.knowledgeuniverse.service;

import com.ftn.research.knowledgeuniverse.model.dto.UserCreationDTO;
import com.ftn.research.knowledgeuniverse.model.entity.User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public interface UserService {
    Optional<User> findById(Long userId);

    boolean isEmailTaken(String email);

    User createUser(UserCreationDTO userCreationDTO);

    Optional<User> findByCredentials(String email, String password);
}
