package com.ftn.research.knowledgeuniverse.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public interface IndexingService {

    String indexDocument(MultipartFile documentFile) throws IOException;
}
