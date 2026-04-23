package com.ftn.research.knowledgeuniverse.model.dto;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record BookDocumentFileDTO(
        MultipartFile file,                  // PDF
        MultipartFile previewImage,           // Optional preview image
        List<Long> genreIds
) {}