package com.ftn.research.knowledgeuniverse.controller;

import com.ftn.research.knowledgeuniverse.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.Map;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin
public class FileController {

    private final FileService fileService;

    /**
     * Download endpoint (PDF, images, any file)
     */
    @GetMapping("/{filename}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {

        InputStream stream = fileService.load(filename);
        String contentType = fileService.getContentType(filename);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(new InputStreamResource(stream));
    }

    /**
     * Image preview endpoint (presigned URL from MinIO)
     */
    @GetMapping("/preview/{filename}")
    public ResponseEntity<Map<String, String>> previewImage(@PathVariable String filename) {

        String contentType = fileService.getContentType(filename);

        if (!contentType.startsWith("image/")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Preview allowed only for image files"));
        }

        String presignedUrl = fileService.getPresignedUrl(filename);

        return ResponseEntity.ok(Map.of("url", presignedUrl));
    }
}