package com.ftn.research.knowledgeuniverse.util;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

public class RAGStreamingChunker {

    // =========================
    // CONFIG
    // =========================
    private static final int OVERLAP_WORDS_RATIO = 30; // ~30% overlap

    public enum Language {
        EN, DE,
        FR, PT, IT, ES,
        SR, RU, UK
    }

    // =========================
    // ABBREVIATIONS
    // =========================
    private static final Map<Language, Set<String>> ABBR = Map.of(
            Language.EN, Set.of("dr.", "mr.", "mrs.", "ms.", "prof.", "e.g.", "i.e.", "etc.", "vs.", "u.s.", "u.k."),
            Language.DE, Set.of("dr.", "prof.", "z.b.", "d.h.", "bzw.", "usw.", "ca."),
            Language.FR, Set.of("m.", "mme.", "dr.", "pr.", "etc.", "p.ex."),
            Language.ES, Set.of("sr.", "sra.", "dr.", "etc.", "p.e."),
            Language.IT, Set.of("sig.", "dott.", "prof.", "ecc."),
            Language.PT, Set.of("sr.", "sra.", "dr.", "etc."),
            Language.SR, Set.of("др.", "проф.", "нпр.", "итд."),
            Language.RU, Set.of("д-р.", "проф.", "т.е.", "и т.д."),
            Language.UK, Set.of("д-р.", "проф.", "і т.д.", "тобто.")
    );

    // =========================
    // THREAD POOL (Elasticsearch indexing)
    // =========================
    private final ExecutorService executor;

    public RAGStreamingChunker(int threads) {
        this.executor = Executors.newFixedThreadPool(threads);
    }

    // =========================
    // PUBLIC API
    // =========================
//    public void chunkAndIndex(Path file, Language lang, int minWords, int maxWords) throws IOException {
//
//        Set<String> abbreviations = ABBR.getOrDefault(lang, ABBR.get(Language.EN));
//
//        try (FileChannel channel = FileChannel.open(file)) {
//
//            ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
//
//            StringBuilder sentence = new StringBuilder(2048);
//            StringBuilder chunk = new StringBuilder(8192);
//            StringBuilder paragraph = new StringBuilder(2048);
//
//            Deque<String> overlapBuffer = new ArrayDeque<>();
//
//            int chunkWords = 0;
//            int sentenceWords = 0;
//            int paragraphWords = 0;
//
//            boolean inWord = false;
//
//            int ch;
//
//            while (channel.read(buffer) > 0) {
//                buffer.flip();
//
//                while (buffer.hasRemaining()) {
//                    char c = (char) buffer.get();
//
//                    sentence.append(c);
//                    paragraph.append(c);
//
//                    if (Character.isWhitespace(c)) {
//                        if (inWord) {
//                            sentenceWords++;
//                            paragraphWords++;
//                            inWord = false;
//                        }
//                    } else {
//                        inWord = true;
//                    }
//
//                    if (isSentenceBoundary(sentence, abbreviations, c)) {
//
//                        if (inWord) {
//                            sentenceWords++;
//                            paragraphWords++;
//                            inWord = false;
//                        }
//
//                        ChunkResult result = addSentenceToChunk(
//                                chunk,
//                                sentence,
//                                chunkWords,
//                                sentenceWords,
//                                paragraphWords,
//                                minWords,
//                                maxWords
//                        );
//
//                        chunkWords = result.chunkWords;
//                        paragraphWords = result.paragraphWords;
//
//                        sentence.setLength(0);
//                        sentenceWords = 0;
//                    }
//
//                    // paragraph boundary (soft)
//                    if (c == '\n') {
//                        paragraph.setLength(0);
//                        paragraphWords = 0;
//                    }
//                }
//
//                buffer.clear();
//            }
//
//            // flush last sentence
//            if (sentence.length() > 0) {
//                if (inWord) sentenceWords++;
//
//                ChunkResult result = addSentenceToChunk(
//                        chunk,
//                        sentence,
//                        chunkWords,
//                        sentenceWords,
//                        paragraphWords,
//                        minWords,
//                        maxWords
//                );
//
//                chunkWords = result.chunkWords;
//            }
//
//            finalizeChunk(chunk, chunkWords, minWords, overlapBuffer, maxWords);
//        }
//
//        executor.shutdown();
//    }

//    TODO new chunkandindex w/ sentence-level language awareness and force chunking on its change
public void chunkAndIndex(Path file, Language lang, int minWords, int maxWords) throws IOException {

    Set<String> abbreviations = ABBR.getOrDefault(lang, ABBR.get(Language.EN));

    try (FileChannel channel = FileChannel.open(file)) {

        ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);

        StringBuilder sentence = new StringBuilder(2048);
        StringBuilder chunk = new StringBuilder(8192);
        StringBuilder paragraph = new StringBuilder(2048);

        int chunkWords = 0;
        int sentenceWords = 0;
        int paragraphWords = 0;

        boolean inWord = false;

        // 🔥 NEW
        String currentChunkLang = null;
        int chunkIndex = 0;

        while (channel.read(buffer) > 0) {
            buffer.flip();

            while (buffer.hasRemaining()) {
                char c = (char) buffer.get();

                sentence.append(c);
                paragraph.append(c);

                if (Character.isWhitespace(c)) {
                    if (inWord) {
                        sentenceWords++;
                        paragraphWords++;
                        inWord = false;
                    }
                } else {
                    inWord = true;
                }

                if (isSentenceBoundary(sentence, abbreviations, c)) {

                    if (inWord) {
                        sentenceWords++;
                        paragraphWords++;
                        inWord = false;
                    }

                    String sentenceText = sentence.toString().trim();

                    // 🔥 OPTIMIZED LANGUAGE DETECTION
                    String sentenceLang;
                    if (sentenceText.length() > 40) {
                        sentenceLang = detectLanguage(sentenceText);
                    } else {
                        sentenceLang = currentChunkLang != null ? currentChunkLang : lang.name();
                    }

                    // 🔥 INIT FIRST LANG
                    if (currentChunkLang == null) {
                        currentChunkLang = sentenceLang;
                    }

                    // 🔥 LANGUAGE SWITCH → FORCE CHUNK BREAK
                    if (!sentenceLang.equals(currentChunkLang)) {

                        if (chunk.length() > 0) {
                            submitChunk(chunk.toString().trim(), currentChunkLang, chunkIndex++);
                        }

                        chunk.setLength(0);
                        chunkWords = 0;

                        currentChunkLang = sentenceLang;
                    }

                    // 🔥 NORMAL CHUNKING
                    ChunkResult result = addSentenceToChunk(
                            chunk,
                            sentence,
                            chunkWords,
                            sentenceWords,
                            paragraphWords,
                            minWords,
                            maxWords
                    );

                    chunkWords = result.chunkWords;
                    paragraphWords = result.paragraphWords;

                    sentence.setLength(0);
                    sentenceWords = 0;
                }

                if (c == '\n') {
                    paragraph.setLength(0);
                    paragraphWords = 0;
                }
            }

            buffer.clear();
        }

        // 🔥 FINAL SENTENCE
        if (sentence.length() > 0) {
            if (inWord) sentenceWords++;

            String sentenceText = sentence.toString().trim();

            String sentenceLang;
            if (sentenceText.length() > 40) {
                sentenceLang = detectLanguage(sentenceText);
            } else {
                sentenceLang = currentChunkLang != null ? currentChunkLang : lang.name();
            }

            if (currentChunkLang == null) {
                currentChunkLang = sentenceLang;
            }

            if (!sentenceLang.equals(currentChunkLang)) {
                if (chunk.length() > 0) {
                    submitChunk(chunk.toString().trim(), currentChunkLang, chunkIndex++);
                }

                chunk.setLength(0);
                chunkWords = 0;
                currentChunkLang = sentenceLang;
            }

            ChunkResult result = addSentenceToChunk(
                    chunk,
                    sentence,
                    chunkWords,
                    sentenceWords,
                    paragraphWords,
                    minWords,
                    maxWords
            );

            chunkWords = result.chunkWords;
        }

        // 🔥 FINAL CHUNK
        if (chunk.length() > 0) {
            submitChunk(chunk.toString().trim(), currentChunkLang, chunkIndex++);
        }
    }
}

    // =========================
    // CHUNK LOGIC (with overlap + paragraph awareness)
    // =========================
    private ChunkResult addSentenceToChunk(
            StringBuilder chunk,
            StringBuilder sentence,
            int chunkWords,
            int sentenceWords,
            int paragraphWords,
            int minWords,
            int maxWords
    ) {

        boolean paragraphBoundaryBonus = paragraphWords == 0;

        int effectiveMax = paragraphBoundaryBonus ? maxWords + 10 : maxWords;

        if (chunkWords + sentenceWords > effectiveMax) {

            // finalize current chunk if valid
            if (chunkWords >= minWords) {
                submitChunk(chunk.toString().trim());
            }

            chunk.setLength(0);
            chunk.append(sentence);

            return new ChunkResult(sentenceWords, paragraphWords);

        } else {
            chunk.append(sentence);
            return new ChunkResult(chunkWords + sentenceWords, paragraphWords);
        }
    }

    private void finalizeChunk(
            StringBuilder chunk,
            int chunkWords,
            int minWords,
            Deque<String> overlapBuffer,
            int maxWords
    ) {

        if (chunk.length() == 0) return;

        String finalChunk = chunk.toString().trim();

        if (chunkWords >= minWords) {
            submitChunk(finalChunk);
        }

        // OVERLAP LOGIC (RAG improvement)
        int overlapWords = chunkWords * OVERLAP_WORDS_RATIO / 100;

        String overlap = extractLastWords(finalChunk, overlapWords);
        overlapBuffer.add(overlap);

        chunk.setLength(0);
    }

    // =========================
    // ELASTICSEARCH INDEXING (async thread per chunk task)
    // =========================
//    private void submitChunk(String chunkText) {
//        executor.submit(() -> indexToElasticsearch(chunkText));
//    }

//    todo old improvement attempt, restart-unsafe counter that would reset
//    private AtomicInteger chunkCounter = new AtomicInteger(0);
//
//    private final BulkIndexingService bulkService;
//    private final String bookId;
//    private final String title;
//    private final String language;
//    private final String isbn;
//
//    private void submitChunk(String chunkText) {
//
//        ChunkDocument doc = new ChunkDocument();
//        doc.setBookId(bookId);
//        doc.setTitle(title);
//        doc.setContent(chunkText);
//        doc.setChunkIndex(chunkCounter.getAndIncrement());
//        doc.setLanguage(language);
//        doc.setDatabaseISBN(isbn);
//
//        bulkService.submit(doc);
//    }

    private void submitChunk(String chunkText, String language, int chunkIndex) {

        ChunkDocument doc = new ChunkDocument();

        doc.setBookId(bookId);
        doc.setTitle(title);
        doc.setChunkIndex(chunkIndex);
        doc.setDatabaseISBN(isbn);

        switch (language) {
            case "SR" -> doc.setContentSr(chunkText);
            case "RU" -> doc.setContentRu(chunkText);
            case "DE" -> doc.setContentDe(chunkText);
            case "FR" -> doc.setContentFr(chunkText);
            case "IT" -> doc.setContentIt(chunkText);
            case "ES" -> doc.setContentEs(chunkText);
            case "PT" -> doc.setContentPt(chunkText);
            case "UK" -> doc.setContentUk(chunkText);
            default -> doc.setContentEn(chunkText);
        }

        bulkService.submit(doc);
    }


    private void indexToElasticsearch(String chunkText) {
        // Replace with real ES client logic
        System.out.println("Indexing chunk (" + chunkText.length() + " chars)");
    }

    // =========================
    // SENTENCE BOUNDARY
    // =========================
    private boolean isSentenceBoundary(StringBuilder sentence, Set<String> abbr, char c) {

        int len = sentence.length();
        if (len < 2) return false;

        if (c == '。' || c == '！' || c == '？') return true;

        char last = c;
        if (isQuote(c) && len >= 2) {
            last = sentence.charAt(len - 2);
        }

        if (last != '.' && last != '!' && last != '?') return false;

        String lower = sentence.toString().toLowerCase().trim();

        for (String a : abbr) {
            if (lower.endsWith(a)) return false;
        }

        if (lower.endsWith("...")) return false;

        return !looksLikeAbbreviation(lower);
    }

    private boolean looksLikeAbbreviation(String text) {
        int i = text.lastIndexOf(' ');
        if (i == -1) return false;

        String w = text.substring(i + 1).replaceAll("[^\\p{L}.]", "");
        return w.length() <= 3 && w.endsWith(".");
    }

    private boolean isQuote(char c) {
        return c == '"' || c == '\'' || c == '”' || c == '’';
    }

    // =========================
    // OVERLAP HELPERS
    // =========================
    private String extractLastWords(String text, int wordCount) {
        String[] words = text.split("\\s+");
        if (words.length <= wordCount) return text;

        StringBuilder sb = new StringBuilder();
        for (int i = words.length - wordCount; i < words.length; i++) {
            sb.append(words[i]).append(' ');
        }
        return sb.toString().trim();
    }

    // =========================
    // RESULT HOLDER
    // =========================
    private static class ChunkResult {
        int chunkWords;
        int paragraphWords;

        ChunkResult(int c, int p) {
            this.chunkWords = c;
            this.paragraphWords = p;
        }
    }
}