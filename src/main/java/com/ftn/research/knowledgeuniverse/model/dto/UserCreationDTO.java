package com.ftn.research.knowledgeuniverse.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserCreationDTO {
    private Long id;
    private String email;
    private String password;
    private List<Long> favoriteGenresIDs;
}
