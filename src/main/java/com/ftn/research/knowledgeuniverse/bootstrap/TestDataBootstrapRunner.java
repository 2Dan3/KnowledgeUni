package com.ftn.research.knowledgeuniverse.bootstrap;

import com.ftn.research.knowledgeuniverse.service.impl.TestDataUploadClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@Profile("dev")
@Component
@RequiredArgsConstructor
@Slf4j
public class TestDataBootstrapRunner implements CommandLineRunner {

    @Autowired
    private final TestDataUploadClient uploadClient;

    @Override
    public void run(String... args) {

        Path root = Paths.get("test-data");

        if (!Files.exists(root)) {
            log.warn("No test-data directory found → skipping bootstrap");
            return;
        }

        log.info("Starting test-data bootstrap upload...");

        try (Stream<Path> paths = Files.walk(root)) {

            paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".pdf"))
                    .forEach(uploadClient::uploadPair);

        } catch (Exception e) {
            log.error("Bootstrap failed", e);
        }

        log.info("Test-data bootstrap completed");
    }
}