package com.ftn.research.knowledgeuniverse.repository.index;

import com.ftn.research.knowledgeuniverse.model.index.BookIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookIndexRepository
        extends ElasticsearchRepository<BookIndex, String> {

}
