package com.simpleblog.service;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import com.simpleblog.model.dto.DocumentSearchPage;
import com.simpleblog.model.entity.Document;
import com.simpleblog.model.entity.DocumentNodeType;
import com.simpleblog.model.entity.DocumentSearchDocument;
import com.simpleblog.repository.DocumentSearchRepository;
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
public class DocumentSearchService {
    private static final int MAX_PAGE_SIZE = 50;
    private final DocumentSearchRepository documentSearchRepository;
    private final ElasticsearchOperations operations;

    public DocumentSearchService(DocumentSearchRepository documentSearchRepository,
                                 ElasticsearchOperations operations) {
        this.documentSearchRepository = documentSearchRepository;
        this.operations = operations;
    }

    public void indexDocument(Document document) {
        if (document == null || document.getId() == null) {
            return;
        }
        DocumentSearchDocument doc = new DocumentSearchDocument();
        doc.setId(document.getId());
        doc.setTitle(document.getTitle());
        doc.setContent(document.getContent());
        doc.setVersion(document.getVersion());
        doc.setPath(document.getPath());
        doc.setType(document.getType() == null ? null : document.getType().getValue());
        doc.setHidden(document.getHidden());
        doc.setCreatedAt(document.getCreatedAt());
        doc.setUpdatedAt(document.getUpdatedAt());
        documentSearchRepository.save(doc);
    }

    public void deleteDocument(Long documentId) {
        if (documentId == null) {
            return;
        }
        documentSearchRepository.deleteById(documentId);
    }

    public DocumentSearchPage search(String query, int page, int size, boolean includeHidden, String version) {
        String normalized = query == null ? "" : query.trim();
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        if (normalized.isEmpty()) {
            return new DocumentSearchPage(List.of(), 0, safePage, safeSize, normalized);
        }
        int offset = safePage - 1;

        var matchQuery = QueryBuilders.multiMatch(m -> m
                .query(normalized)
                .fields("title^3", "content")
        );

        var boolQuery = QueryBuilders.bool(b -> {
            b.must(matchQuery);
            b.filter(QueryBuilders.term(t -> t.field("type").value(DocumentNodeType.DOC.getValue())));
            b.filter(QueryBuilders.term(t -> t.field("doc_version").value(version)));
            if (!includeHidden) {
                b.filter(QueryBuilders.term(t -> t.field("is_hidden").value(false)));
            }
            return b;
        });

        NativeQuery esQuery = NativeQuery.builder()
                .withQuery(boolQuery)
                .withPageable(PageRequest.of(offset, safeSize))
                .build();

        SearchHits<DocumentSearchDocument> hits = operations.search(esQuery, DocumentSearchDocument.class);
        List<Document> items = new ArrayList<>();
        for (SearchHit<DocumentSearchDocument> hit : hits) {
            DocumentSearchDocument source = hit.getContent();
            Document doc = new Document();
            doc.setId(source.getId());
            doc.setTitle(buildHighlight(source.getTitle(), normalized));
            doc.setContent(snippetHighlight(source.getContent(), normalized, 160));
            doc.setVersion(source.getVersion());
            doc.setPath(source.getPath());
            doc.setHidden(Boolean.TRUE.equals(source.getHidden()));
            doc.setType(DocumentNodeType.fromValue(source.getType()));
            doc.setCreatedAt(source.getCreatedAt());
            doc.setUpdatedAt(source.getUpdatedAt());
            items.add(doc);
        }
        return new DocumentSearchPage(items, hits.getTotalHits(), safePage, safeSize, normalized);
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
}
