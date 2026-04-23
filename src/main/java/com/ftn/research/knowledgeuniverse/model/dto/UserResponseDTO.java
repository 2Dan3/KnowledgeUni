package com.ftn.research.knowledgeuniverse.model.dto;

import com.ftn.research.knowledgeuniverse.model.entity.User;
import lombok.Data;

public class UserResponseDTO {
    private Long id;
    private String email;

    public UserResponseDTO(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
    }
}
