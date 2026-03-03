package com.ftn.research.knowledgeuniverse.model.dto;

import org.springframework.web.multipart.MultipartFile;

public record BookDocumentFileDTO(
        MultipartFile file,                  // PDF
        MultipartFile previewImage           // Optional preview image
) {}