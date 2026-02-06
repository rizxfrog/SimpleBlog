package com.simpleblog.service;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import com.simpleblog.repository.BlogSearchRepository;
import com.simpleblog.model.entity.Blog;
import com.simpleblog.model.entity.BlogDocument;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class BlogSearchService {
    private static final int MAX_PAGE_SIZE = 50;
    private final BlogSearchRepository blogSearchRepository;
    private final ElasticsearchOperations operations;

    public BlogSearchService(BlogSearchRepository blogSearchRepository,
                             ElasticsearchOperations operations) {
        this.blogSearchRepository = blogSearchRepository;
        this.operations = operations;
    }

    public void indexBlog(Blog blog) {
        if (blog == null || blog.getId() == null) {
            return;
        }
        if (Boolean.FALSE.equals(blog.getPublished())) {
            blogSearchRepository.deleteById(blog.getId());
            return;
        }
        BlogDocument doc = new BlogDocument();
        doc.setId(blog.getId());
        doc.setTitle(blog.getTitle());
        doc.setSummary(blog.getSummary());
        doc.setContent(blog.getContent());
        doc.setAuthorId(blog.getAuthorId());
        doc.setCategoryId(blog.getCategoryId());
        doc.setCoverUrl(blog.getCoverUrl());
        doc.setPublished(blog.getPublished());
        doc.setCreatedAt(blog.getCreatedAt());
        doc.setUpdatedAt(blog.getUpdatedAt());
        blogSearchRepository.save(doc);
    }

    public void deleteBlog(Long blogId) {
        if (blogId == null) {
            return;
        }
        blogSearchRepository.deleteById(blogId);
    }

    public SearchResult search(String query, int page, int size) {
        String normalized = query == null ? "" : query.trim();
        if (normalized.isEmpty()) {
            return new SearchResult(List.of(), 0);
        }
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int offset = safePage - 1;

        var matchQuery = QueryBuilders.multiMatch(m -> m
                .query(normalized)
                .fields("title^3", "summary^2", "content")
        );
        var boolQuery = QueryBuilders.bool(b -> b
                .must(matchQuery)
                .filter(QueryBuilders.term(t -> t.field("is_published").value(true)))
        );

        NativeQuery esQuery = NativeQuery.builder()
                .withQuery(boolQuery)
                .withPageable(PageRequest.of(offset, safeSize))
                .build();

        SearchHits<BlogDocument> hits = operations.search(esQuery, BlogDocument.class);
        List<Blog> items = new ArrayList<>();
        for (SearchHit<BlogDocument> hit : hits) {
            BlogDocument doc = hit.getContent();
            Blog blog = new Blog();
            blog.setId(doc.getId());
            blog.setTitle(doc.getTitle());
            blog.setSummary(doc.getSummary());
            blog.setContent(doc.getContent());
            blog.setAuthorId(doc.getAuthorId());
            blog.setCategoryId(doc.getCategoryId());
            blog.setCoverUrl(doc.getCoverUrl());
            blog.setPublished(doc.getPublished());
            blog.setCreatedAt(doc.getCreatedAt());
            blog.setUpdatedAt(doc.getUpdatedAt());
            blog.setTitleHighlight(buildHighlight(doc.getTitle(), normalized));
            blog.setSummaryHighlight(resolveSummaryHighlight(doc, normalized));
            items.add(blog);
        }
        return new SearchResult(items, hits.getTotalHits());
    }

    private String resolveSummaryHighlight(BlogDocument doc, String query) {
        if (doc.getSummary() != null && !doc.getSummary().isBlank()) {
            return buildHighlight(doc.getSummary(), query);
        }
        if (doc.getContent() == null) {
            return null;
        }
        return snippetHighlight(doc.getContent(), query, 120);
    }

    private String buildHighlight(String text, String query) {
        if (text == null || query == null || query.isBlank()) {
            return text;
        }
        String result = text;
        for (String token : query.split("\\s+")) {
            if (token.isBlank()) {
                continue;
            }
            result = result.replaceAll("(?i)" + java.util.regex.Pattern.quote(token), "<mark>$0</mark>");
        }
        return result;
    }

    private String snippetHighlight(String text, String query, int maxLen) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        String[] tokens = query.toLowerCase(Locale.ROOT).split("\\s+");
        int index = -1;
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            index = lower.indexOf(token);
            if (index >= 0) {
                break;
            }
        }
        if (index < 0) {
            return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
        }
        int start = Math.max(index - maxLen / 2, 0);
        int end = Math.min(start + maxLen, text.length());
        String snippet = text.substring(start, end);
        return buildHighlight(snippet, query) + (end < text.length() ? "..." : "");
    }

    public record SearchResult(List<Blog> items, long total) {}
}
