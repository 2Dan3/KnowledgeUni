package com.ftn.research.knowledgeuniverse.controller;

import com.ftn.research.knowledgeuniverse.model.dto.BookDocumentFileDTO;
import com.ftn.research.knowledgeuniverse.model.dto.BookDocumentFileResponseDTO;
import com.ftn.research.knowledgeuniverse.service.FileService;
import com.ftn.research.knowledgeuniverse.service.IndexingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

@RestController
@RequestMapping("/api/index")
@RequiredArgsConstructor
public class ESIndexController {

    private final IndexingService indexingService;
    private final FileService fileService;

    /**
     * Upload PDF with optional preview image
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookDocumentFileResponseDTO addDocumentFile(@ModelAttribute BookDocumentFileDTO documentFile) {

        // Store the PDF file
        var pdfServerFilename = indexingService.indexDocument(documentFile.file());

        // Optionally store preview image if provided
        MultipartFile previewImage = documentFile.previewImage(); // new optional field
        if (previewImage != null && !previewImage.isEmpty()) {

            // Construct preview filename, link it to PDF
            String extension = Objects.requireNonNull(previewImage.getOriginalFilename())
                    .substring(previewImage.getOriginalFilename().lastIndexOf(".") + 1);
            String previewServerFilename = pdfServerFilename.substring(0, pdfServerFilename.lastIndexOf('.')) + "_preview";

            fileService.store(previewImage, previewServerFilename);
//            System.out.println(pdfServerFilename + " " + previewServerFilename + " " + extension);
        }

        return new BookDocumentFileResponseDTO(pdfServerFilename);
    }
}