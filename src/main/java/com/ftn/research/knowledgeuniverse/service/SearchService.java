package com.ftn.research.knowledgeuniverse.service;

import com.ftn.research.knowledgeuniverse.model.index.BookIndex;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface SearchService {

    Page<BookIndex> simpleSearch(List<String> keywords, Pageable pageable, boolean isKNN);

    Page<BookIndex> advancedSearch(List<String> expression, Pageable pageable);
}
