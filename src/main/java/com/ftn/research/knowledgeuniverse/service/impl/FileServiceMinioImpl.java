package com.ftn.research.knowledgeuniverse.service.impl;

import com.ftn.research.knowledgeuniverse.exceptionhandling.exception.NotFoundException;
import com.ftn.research.knowledgeuniverse.exceptionhandling.exception.StorageException;
import com.ftn.research.knowledgeuniverse.service.FileService;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class FileServiceMinioImpl implements FileService {

    private final MinioClient minioClient;

    @Value("${spring.minio.bucket}")
    private String bucketName;

    @Override
    public String store(MultipartFile file, String serverFilename) {
        if (file.isEmpty()) {
            throw new StorageException("Cannot store empty file.");
        }

        try {
            String originalFilename = Objects.requireNonNull(file.getOriginalFilename());
            String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
            String finalName = serverFilename + "." + extension;

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(finalName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            return finalName;

        } catch (Exception e) {
            throw new StorageException("Error storing file in MinIO: " + e.getMessage());
        }
    }

    @Override
    public InputStream load(String serverFilename) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(serverFilename)
                            .build()
            );
        } catch (Exception e) {
            throw new NotFoundException("File not found: " + serverFilename);
        }
    }

    @Override
    public String getPresignedUrl(String serverFilename) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(serverFilename)
                            .expiry(5, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            throw new NotFoundException("File not found: " + serverFilename);
        }
    }

    @Override
    public String getContentType(String serverFilename) {
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(serverFilename)
                            .build()
            );
            return stat.contentType() != null ? stat.contentType() : "application/octet-stream";
        } catch (Exception e) {
            return "application/octet-stream";
        }
    }
}