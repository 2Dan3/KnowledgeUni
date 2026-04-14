package com.ftn.research.knowledgeuniverse.service.impl;

import ai.djl.translate.TranslateException;
import com.ftn.research.knowledgeuniverse.exceptionhandling.exception.LoadingException;
import com.ftn.research.knowledgeuniverse.exceptionhandling.exception.StorageException;
import com.ftn.research.knowledgeuniverse.model.entity.Book;
import com.ftn.research.knowledgeuniverse.model.index.BookIndex;
import com.ftn.research.knowledgeuniverse.repository.entity.BookRepository;
import com.ftn.research.knowledgeuniverse.repository.index.BookIndexRepository;
import com.ftn.research.knowledgeuniverse.service.FileService;
import com.ftn.research.knowledgeuniverse.service.IndexingService;
import com.ftn.research.knowledgeuniverse.service.impl.EmbeddingService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.Tika;
import org.apache.tika.language.detect.LanguageDetector;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingServiceImpl implements IndexingService {

    private final BookIndexRepository bookIndexRepository;

    private final BookRepository bookRepository;

    private final FileService fileService;

    private final LanguageDetector languageDetector;

    private final EmbeddingService embeddingService;

//    @Override
//    @Transactional
//    public String indexDocument(MultipartFile documentFile) {
//        var newEntity = new Book();
//        var newIndex = new BookIndex();
//
//        var title = Objects.requireNonNull(documentFile.getOriginalFilename()).split("\\.")[0];
//        newIndex.setTitle(title);
//        newEntity.setTitle(title);
//
////        hr/sr, en, de, fr, ru, uk, es, it, pt)
//        var documentContent = extractDocumentContent(documentFile);
//        var detectedLang = detectLanguage(documentContent);
//
//        switch (detectedLang) {
//            case "SR" -> newIndex.setContentSr(documentContent);
//            case "RU" -> newIndex.setContentRu(documentContent);
//            case "DE" -> newIndex.setContentDe(documentContent);
//            case "FR" -> newIndex.setContentFr(documentContent);
//            case "IT" -> newIndex.setContentIt(documentContent);
//            case "ES" -> newIndex.setContentEs(documentContent);
//            case "PT" -> newIndex.setContentPt(documentContent);
//            case "UK" -> newIndex.setContentUk(documentContent);
//            default -> newIndex.setContentEn(documentContent);
//        }
//
//        newEntity.setContentInNativeLang(documentContent);
//
//        var serverFilename = fileService.store(documentFile, UUID.randomUUID().toString());
//        newIndex.setServerFilename(serverFilename);
//        newEntity.setServerFilename(serverFilename);
//
//        newEntity.setMimeType(detectMimeType(documentFile));
//        var savedEntity = bookRepository.save(newEntity);
//
//        try {
//            newIndex.setVectorizedContent(embeddingService.getEmbedding(title));
//        } catch (TranslateException e) {
////            log.error("Could not calculate vector representation for document with ID: {}",
////                savedEntity.getISBN();
//        }
//        newIndex.setDatabaseISBN(savedEntity.getIsbn());
//        bookIndexRepository.save(newIndex);
//
//        return serverFilename;
//    }

//    todo replace setcontenten single field with this
//    switch (lang) {
//        case "SR" -> doc.setContentSr(chunkText);
//        case "RU" -> doc.setContentRu(chunkText);
//        case "DE" -> doc.setContentDe(chunkText);
//        case "FR" -> doc.setContentFr(chunkText);
//        case "IT" -> doc.setContentIt(chunkText);
//        case "ES" -> doc.setContentEs(chunkText);
//        case "PT" -> doc.setContentPt(chunkText);
//        case "UK" -> doc.setContentUk(chunkText);
//        default -> doc.setContentEn(chunkText);
//    }

    @Override
    @Transactional
    public String indexDocument(MultipartFile documentFile) {

        var book = new Book();

        String title = documentFile.getOriginalFilename().split("\\.")[0];
        book.setTitle(title);

//        TODO remove content to DataBase: duplication with ES not needed
        String content = extractDocumentContent(documentFile);
        String lang = detectLanguage(content);

//        TODO uncomment - set book's majority language
//        book.setLanguage(lang);

        String serverFilename = fileService.store(documentFile, UUID.randomUUID().toString());
        book.setServerFilename(serverFilename);
        book.setMimeType(detectMimeType(documentFile));

        var saved = bookRepository.save(book);

        // 🔥 STREAM → CHUNK → QUEUE → BULK
        RAGStreamingChunker chunker = new RAGStreamingChunker(
                bulkIndexingService,
                saved.getIsbn(),
                title,
                lang
        );

        chunker.chunkAndIndex(
                fileService.loadAsPath(serverFilename),
                RAGStreamingChunker.Language.valueOf(lang),
                80,
                120
        );
//        TODO try 80- 120/200 words instead of 100-300 min-max

        return serverFilename;
    }

    private String extractDocumentContent(MultipartFile multipartPdfFile) {
        String documentContent;
        try (var pdfFile = multipartPdfFile.getInputStream()) {
            var pdDocument = PDDocument.load(pdfFile);
            var textStripper = new PDFTextStripper();
//            textStripper.setAddMoreFormatting(false);
            documentContent = textStripper.getText(pdDocument);
            pdDocument.close();
        } catch (IOException e) {
            System.out.println("\n\nError while loading PDF file content.\n\n");
            throw new LoadingException("Error while trying to load PDF file content.");
        }

        return documentContent;
    }

    private String detectLanguage(String text) {
        var detectedLanguage = languageDetector.detect(text).getLanguage().toUpperCase();
        if (detectedLanguage.equals("HR")) {
            detectedLanguage = "SR";
        }

        return detectedLanguage;
    }

    private String detectMimeType(MultipartFile file) {
        var contentAnalyzer = new Tika();

        String trueMimeType;
        String specifiedMimeType;
        try {
            trueMimeType = contentAnalyzer.detect(file.getBytes());
            specifiedMimeType =
                Files.probeContentType(Path.of(Objects.requireNonNull(file.getOriginalFilename())));
        } catch (IOException e) {
            throw new StorageException("Failed to detect mime type for file.");
        }

        if (!trueMimeType.equals(specifiedMimeType) &&
            !(trueMimeType.contains("zip") && specifiedMimeType.contains("zip"))) {
            throw new StorageException("True mime type is different from specified one, aborting.");
        }

        return trueMimeType;
    }
}