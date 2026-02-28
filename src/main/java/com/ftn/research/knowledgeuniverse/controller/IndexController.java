package com.ftn.research.knowledgeuniverse.controller;

import com.ftn.research.knowledgeuniverse.model.dto.BookDocumentFileDTO;
import com.ftn.research.knowledgeuniverse.model.dto.BookDocumentFileResponseDTO;
import com.ftn.research.knowledgeuniverse.service.IndexingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/index")
@RequiredArgsConstructor
public class IndexController {

    private final IndexingService indexingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookDocumentFileResponseDTO addDocumentFile(
        @ModelAttribute BookDocumentFileDTO documentFile) {
        var serverFilename = indexingService.indexDocument(documentFile.file());
        return new BookDocumentFileResponseDTO(serverFilename);
    }
}
