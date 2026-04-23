package com.ftn.research.knowledgeuniverse.service;

import com.ftn.research.knowledgeuniverse.model.dto.BookDocumentFileDTO;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public interface IndexingService {

    String indexDocument(BookDocumentFileDTO documentFile) throws IOException;
}
