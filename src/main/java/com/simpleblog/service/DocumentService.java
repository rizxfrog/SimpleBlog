package com.simpleblog.service;

import com.simpleblog.mapper.DocumentMapper;
import com.simpleblog.model.dto.DocumentCreateInput;
import com.simpleblog.model.dto.DocumentMoveInput;
import com.simpleblog.model.dto.DocumentSearchPage;
import com.simpleblog.model.dto.DocumentUpdateInput;
import com.simpleblog.model.entity.Document;
import com.simpleblog.model.entity.DocumentNodeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class DocumentService {
    private final DocumentMapper documentMapper;
    private final DocumentSearchService documentSearchService;
    private final DocumentRevisionService documentRevisionService;

    public DocumentService(DocumentMapper documentMapper,
                           DocumentSearchService documentSearchService,
                           DocumentRevisionService documentRevisionService) {
        this.documentMapper = documentMapper;
        this.documentSearchService = documentSearchService;
        this.documentRevisionService = documentRevisionService;
    }

    public Document findById(Long id, boolean includeHidden) {
        if (id == null) {
            return null;
        }
        Document document = documentMapper.selectById(id);
        if (document == null) {
            return null;
        }
        if (!includeHidden && Boolean.TRUE.equals(document.getHidden())) {
            return null;
        }
        return document;
    }

    public List<Document> listDocuments(boolean includeHidden) {
        return documentMapper.listAll(includeHidden);
    }

    public List<Document> documentTree(Long rootId, boolean includeHidden) {
        List<Document> nodes;
        if (rootId == null) {
            nodes = documentMapper.listAll(includeHidden);
            return buildTree(nodes, null);
        }
        String rootPath = documentMapper.findPathById(rootId);
        if (rootPath == null) {
            return List.of();
        }
        nodes = documentMapper.listSubtree(rootPath, includeHidden);
        return buildTree(nodes, rootId);
    }

    public DocumentSearchPage searchDocuments(String query, int page, int size, boolean includeHidden) {
        return documentSearchService.search(query, page, size, includeHidden);
    }

    @Transactional
    public Document create(DocumentCreateInput input, Long userId) {
        if (input == null) {
            throw new IllegalArgumentException("Input is required.");
        }
        DocumentNodeType type = input.type() == null ? DocumentNodeType.DOC : input.type();
        String title = safeTitle(input.title());
        String slug = normalizeSlug(input.slug(), title);
        String path = buildPath(input.parentId(), slug);

        Document document = new Document();
        document.setParentId(input.parentId());
        document.setType(type);
        document.setTitle(title);
        document.setContent(type == DocumentNodeType.DOC ? input.content() : null);
        document.setPath(path);
        document.setSortOrder(input.sortOrder() == null ? 0 : input.sortOrder());
        document.setHidden(Boolean.TRUE.equals(input.hidden()));
        document.setCreatedBy(userId);
        document.setUpdatedBy(userId);
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.insert(document);
        Document created = documentMapper.selectById(document.getId());
        documentSearchService.indexDocument(created);
        return created;
    }

    @Transactional
    public Document update(Long id, DocumentUpdateInput input, Long userId) {
        Document document = documentMapper.selectById(id);
        if (document == null) {
            throw new IllegalArgumentException("Document not found: " + id);
        }
        documentRevisionService.recordRevision(document, userId);

        if (input.title() != null) {
            document.setTitle(safeTitle(input.title()));
        }
        if (input.sortOrder() != null) {
            document.setSortOrder(input.sortOrder());
        }
        if (input.hidden() != null) {
            document.setHidden(input.hidden());
        }
        if (input.type() != null) {
            document.setType(input.type());
        }
        DocumentNodeType nextType = input.type() != null ? input.type() : document.getType();
        if (input.content() != null) {
            document.setContent(nextType == DocumentNodeType.DOC ? input.content() : null);
        }
        if (nextType == DocumentNodeType.FOLDER) {
            document.setContent(null);
        }
        document.setUpdatedBy(userId);
        document.setUpdatedAt(LocalDateTime.now());

        String newSlug = normalizeSlug(input.slug(), document.getTitle());
        boolean pathChanged = false;
        if (input.slug() != null) {
            renamePath(document, newSlug);
            pathChanged = true;
        }

        documentMapper.updateById(document);
        Document updated = documentMapper.selectById(id);
        documentSearchService.indexDocument(updated);
        if (pathChanged) {
            reindexSubtree(updated.getPath());
        }
        return updated;
    }

    @Transactional
    public Document move(Long id, DocumentMoveInput input, Long userId) {
        Document document = documentMapper.selectById(id);
        if (document == null) {
            throw new IllegalArgumentException("Document not found: " + id);
        }
        documentRevisionService.recordRevision(document, userId);

        Long newParentId = input.parentId();
        String fallbackSlug = document.getSlug() == null ? document.getTitle() : document.getSlug();
        String slug = normalizeSlug(input.slug(), fallbackSlug);

        String oldPath = document.getPath();
        if (newParentId != null) {
            long invalid = documentMapper.countDescendantOf(newParentId, oldPath);
            if (invalid > 0) {
                throw new IllegalArgumentException("Cannot move a document under its descendant.");
            }
        }
        String newPath = buildPath(newParentId, slug);

        if (!Objects.equals(oldPath, newPath)) {
            documentMapper.updatePathPrefix(oldPath, newPath);
            document.setPath(newPath);
        }

        document.setParentId(newParentId);
        if (input.sortOrder() != null) {
            document.setSortOrder(input.sortOrder());
        }
        document.setUpdatedBy(userId);
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);
        Document updated = documentMapper.selectById(id);
        documentSearchService.indexDocument(updated);
        reindexSubtree(updated.getPath());
        return updated;
    }

    @Transactional
    public Document setHidden(Long id, boolean hidden, Long userId) {
        Document document = documentMapper.selectById(id);
        if (document == null) {
            throw new IllegalArgumentException("Document not found: " + id);
        }
        documentRevisionService.recordRevision(document, userId);
        document.setHidden(hidden);
        document.setUpdatedBy(userId);
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);
        Document updated = documentMapper.selectById(id);
        documentSearchService.indexDocument(updated);
        return updated;
    }

    @Transactional
    public boolean delete(Long id, Long userId) {
        Document document = documentMapper.selectById(id);
        List<Document> subtree = List.of();
        if (document != null) {
            documentRevisionService.recordRevision(document, userId);
            subtree = documentMapper.listSubtree(document.getPath(), true);
        }
        documentMapper.deleteById(id);
        if (subtree.isEmpty()) {
            documentSearchService.deleteDocument(id);
        } else {
            for (Document item : subtree) {
                documentSearchService.deleteDocument(item.getId());
            }
        }
        return true;
    }

    private List<Document> buildTree(List<Document> nodes, Long rootId) {
        Map<Long, Document> map = new LinkedHashMap<>();
        for (Document node : nodes) {
            node.setChildren(new ArrayList<>());
            map.put(node.getId(), node);
        }
        List<Document> roots = new ArrayList<>();
        for (Document node : nodes) {
            Long parentId = node.getParentId();
            if (parentId != null && map.containsKey(parentId)) {
                map.get(parentId).getChildren().add(node);
            } else {
                roots.add(node);
            }
        }
        for (Document node : map.values()) {
            node.getChildren().sort(Comparator
                    .comparing(Document::getSortOrder)
                    .thenComparing(Document::getTitle, String.CASE_INSENSITIVE_ORDER));
        }
        roots.sort(Comparator
                .comparing(Document::getSortOrder)
                .thenComparing(Document::getTitle, String.CASE_INSENSITIVE_ORDER));

        if (rootId == null) {
            return roots;
        }
        Document root = map.get(rootId);
        return root == null ? List.of() : List.of(root);
    }

    private String buildPath(Long parentId, String slug) {
        if (parentId == null) {
            return slug;
        }
        String parentPath = documentMapper.findPathById(parentId);
        if (parentPath == null) {
            throw new IllegalArgumentException("Parent document not found: " + parentId);
        }
        return parentPath + "." + slug;
    }

    private void renamePath(Document document, String newSlug) {
        String oldPath = document.getPath();
        String basePath;
        if (document.getParentId() == null) {
            basePath = newSlug;
        } else {
            String parentPath = documentMapper.findPathById(document.getParentId());
            if (parentPath == null) {
                throw new IllegalArgumentException("Parent document not found: " + document.getParentId());
            }
            basePath = parentPath + "." + newSlug;
        }
        if (!Objects.equals(oldPath, basePath)) {
            documentMapper.updatePathPrefix(oldPath, basePath);
            document.setPath(basePath);
        }
    }

    private String safeTitle(String title) {
        if (title == null || title.isBlank()) {
            return "Untitled";
        }
        return title.trim();
    }

    private String normalizeSlug(String slug, String fallbackTitle) {
        String raw = slug == null || slug.isBlank() ? fallbackTitle : slug;
        String normalized = raw
                .trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");
        if (normalized.isEmpty()) {
            return "node";
        }
        return normalized;
    }

    private void reindexSubtree(String rootPath) {
        if (rootPath == null || rootPath.isBlank()) {
            return;
        }
        List<Document> subtree = documentMapper.listSubtree(rootPath, true);
        for (Document item : subtree) {
            documentSearchService.indexDocument(item);
        }
    }
}
