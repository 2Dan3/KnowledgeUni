package com.ftn.research.knowledgeuniverse.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Service
public interface FileService {

    String store(MultipartFile file, String serverFilename);

    InputStream load(String serverFilename);

    String getPresignedUrl(String serverFilename);

    String getContentType(String serverFilename);
}