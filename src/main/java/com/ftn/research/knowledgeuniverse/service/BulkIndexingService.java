package com.ftn.research.knowledgeuniverse.service;

import com.ftn.research.knowledgeuniverse.model.index.BookIndex;
import org.springframework.stereotype.Service;

@Service
public interface BulkIndexingService {
    void submit(BookIndex doc);
}
