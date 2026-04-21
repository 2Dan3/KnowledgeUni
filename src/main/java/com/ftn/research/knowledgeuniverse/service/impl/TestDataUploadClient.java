package com.ftn.research.knowledgeuniverse.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestDataUploadClient {

    @Autowired
    private final RestTemplate restTemplate;

    private static final String API_URL = "http://localhost:8080/api/index";

    public void uploadPair(Path pdfPath) {

        try {
            String baseName = com.google.common.io.Files.getNameWithoutExtension(pdfPath.toString());

            Path imagePathJpg = pdfPath.resolveSibling(baseName + ".jpg");
            Path imagePathPng = pdfPath.resolveSibling(baseName + ".png");

            Path imagePath = Files.exists(imagePathJpg) ? imagePathJpg :
                    Files.exists(imagePathPng) ? imagePathPng :
                            null;

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            body.add("file", toResource(pdfPath));

            if (imagePath != null) {
                body.add("previewImage", toResource(imagePath));
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> request =
                    new HttpEntity<>(body, headers);

            restTemplate.postForEntity(API_URL, request, String.class);

            log.info("Uploaded: {}", pdfPath.getFileName());

        } catch (Exception e) {
            log.error("Failed uploading: " + pdfPath, e);
        }
    }

    private Resource toResource(Path path) throws IOException {
        return new FileSystemResource(path.toFile());
    }
}