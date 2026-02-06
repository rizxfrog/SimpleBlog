package com.simpleblog.repository;

import com.simpleblog.model.entity.BlogDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface BlogSearchRepository extends ElasticsearchRepository<BlogDocument, Long> {
}
