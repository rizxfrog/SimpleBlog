package com.simpleblog.repository;

import com.simpleblog.model.entity.DocumentSearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface DocumentSearchRepository extends ElasticsearchRepository<DocumentSearchDocument, Long> {
}
