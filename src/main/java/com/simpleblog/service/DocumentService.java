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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class DocumentService {
    public static final String DEFAULT_VERSION = "default";
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

    public Document findById(Long id, boolean includeHidden, String version) {
        if (id == null) {
            return null;
        }
        String resolvedVersion = normalizeVersion(version);
        Document document = documentMapper.selectById(id);
        if (document == null) {
            return null;
        }
        if (!Objects.equals(document.getVersion(), resolvedVersion)) {
            return null;
        }
        if (!includeHidden && Boolean.TRUE.equals(document.getHidden())) {
            return null;
        }
        return document;
    }

    public List<Document> listDocuments(boolean includeHidden, String version) {
        String resolvedVersion = normalizeVersion(version);
        return documentMapper.listAll(includeHidden, resolvedVersion);
    }

    public List<Document> documentTree(Long rootId, boolean includeHidden, String version) {
        String resolvedVersion = normalizeVersion(version);
        List<Document> nodes;
        if (rootId == null) {
            nodes = documentMapper.listAll(includeHidden, resolvedVersion);
            return buildTree(nodes, null);
        }
        Document root = documentMapper.selectById(rootId);
        if (root == null || !Objects.equals(root.getVersion(), resolvedVersion)) {
            return List.of();
        }
        nodes = documentMapper.listSubtree(root.getPath(), includeHidden, resolvedVersion);
        return buildTree(nodes, rootId);
    }

    public DocumentSearchPage searchDocuments(String query, int page, int size, boolean includeHidden, String version) {
        String resolvedVersion = normalizeVersion(version);
        return documentSearchService.search(query, page, size, includeHidden, resolvedVersion);
    }

    public List<String> listVersions() {
        List<String> versions = new ArrayList<>();
        versions.add(DEFAULT_VERSION);
        for (String item : documentMapper.listVersions()) {
            String normalized = normalizeVersion(item);
            if (!versions.contains(normalized)) {
                versions.add(normalized);
            }
        }
        versions.sort(String::compareTo);
        versions.remove(DEFAULT_VERSION);
        versions.add(0, DEFAULT_VERSION);
        return versions;
    }

    @Transactional
    public boolean createVersion(String sourceVersion, String targetVersion, Long userId) {
        String source = normalizeVersion(sourceVersion);
        String target = normalizeVersion(targetVersion);
        if (Objects.equals(source, target)) {
            throw new IllegalArgumentException("Source and target versions must be different.");
        }
        if (documentMapper.countByVersion(target) > 0) {
            throw new IllegalArgumentException("Version already exists: " + target);
        }
        List<Document> sourceNodes = documentMapper.listAll(true, source);
        if (sourceNodes.isEmpty()) {
            return true;
        }

        sourceNodes.sort(Comparator
                .comparing((Document node) -> node.getDepth() == null ? 0 : node.getDepth())
                .thenComparing(Document::getSortOrder)
                .thenComparing(Document::getId));

        Map<Long, Long> idMap = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        for (Document sourceNode : sourceNodes) {
            Long mappedParentId = null;
            if (sourceNode.getParentId() != null) {
                mappedParentId = idMap.get(sourceNode.getParentId());
            }
            String slug = deriveSlug(sourceNode.getPath());

            Document next = new Document();
            next.setParentId(mappedParentId);
            next.setType(sourceNode.getType());
            next.setTitle(sourceNode.getTitle());
            next.setContent(sourceNode.getType() == DocumentNodeType.DOC ? sourceNode.getContent() : null);
            next.setVersion(target);
            next.setPath(buildPath(mappedParentId, slug, target));
            next.setSortOrder(sourceNode.getSortOrder());
            next.setHidden(sourceNode.getHidden());
            next.setCreatedBy(userId);
            next.setUpdatedBy(userId);
            next.setCreatedAt(now);
            next.setUpdatedAt(now);
            documentMapper.insert(next);

            Document created = documentMapper.selectById(next.getId());
            documentSearchService.indexDocument(created);
            idMap.put(sourceNode.getId(), next.getId());
        }
        return true;
    }

    @Transactional
    public boolean deleteVersion(String version) {
        String target = normalizeVersion(version);
        if (DEFAULT_VERSION.equals(target)) {
            throw new IllegalArgumentException("Cannot delete default version.");
        }
        List<Document> toDelete = documentMapper.listAllByVersion(target);
        if (toDelete.isEmpty()) {
            return true;
        }
        documentMapper.deleteByVersion(target);
        for (Document item : toDelete) {
            documentSearchService.deleteDocument(item.getId());
        }
        return true;
    }

    @Transactional
    public Document create(DocumentCreateInput input, Long userId) {
        if (input == null) {
            throw new IllegalArgumentException("Input is required.");
        }
        String version = normalizeVersion(input.version());
        DocumentNodeType type = input.type() == null ? DocumentNodeType.DOC : input.type();
        String title = safeTitle(input.title());
        String slug = normalizeSlug(input.slug(), title);
        String path = buildPath(input.parentId(), slug, version);

        Document document = new Document();
        document.setParentId(input.parentId());
        document.setType(type);
        document.setTitle(title);
        document.setContent(type == DocumentNodeType.DOC ? input.content() : null);
        document.setVersion(version);
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
            reindexSubtree(updated.getPath(), updated.getVersion());
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
        String version = document.getVersion();

        String oldPath = document.getPath();
        if (newParentId != null) {
            long invalid = documentMapper.countDescendantOf(newParentId, oldPath, version);
            if (invalid > 0) {
                throw new IllegalArgumentException("Cannot move a document under its descendant.");
            }
        }
        String newPath = buildPath(newParentId, slug, version);

        if (!Objects.equals(oldPath, newPath)) {
            documentMapper.updatePathPrefix(oldPath, newPath, version);
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
        reindexSubtree(updated.getPath(), version);
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
            subtree = documentMapper.listSubtree(document.getPath(), true, document.getVersion());
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

    private String buildPath(Long parentId, String slug, String version) {
        if (parentId == null) {
            return slug;
        }
        String parentPath = documentMapper.findPathByIdAndVersion(parentId, version);
        if (parentPath == null) {
            throw new IllegalArgumentException("Parent document not found: " + parentId);
        }
        return parentPath + "." + slug;
    }

    private void renamePath(Document document, String newSlug) {
        String oldPath = document.getPath();
        String basePath;
        String version = document.getVersion();
        if (document.getParentId() == null) {
            basePath = newSlug;
        } else {
            String parentPath = documentMapper.findPathByIdAndVersion(document.getParentId(), version);
            if (parentPath == null) {
                throw new IllegalArgumentException("Parent document not found: " + document.getParentId());
            }
            basePath = parentPath + "." + newSlug;
        }
        if (!Objects.equals(oldPath, basePath)) {
            documentMapper.updatePathPrefix(oldPath, basePath, version);
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

    private String normalizeVersion(String version) {
        String raw = (version == null || version.isBlank()) ? DEFAULT_VERSION : version;
        String normalized = raw
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-+|-+$", "");
        if (normalized.isEmpty()) {
            return DEFAULT_VERSION;
        }
        return normalized;
    }

    private String deriveSlug(String path) {
        if (path == null || path.isBlank()) {
            return "node";
        }
        int idx = path.lastIndexOf('.');
        String slug = idx >= 0 ? path.substring(idx + 1) : path;
        return slug.isBlank() ? "node" : slug;
    }

    private void reindexSubtree(String rootPath, String version) {
        if (rootPath == null || rootPath.isBlank()) {
            return;
        }
        List<Document> subtree = documentMapper.listSubtree(rootPath, true, version);
        for (Document item : subtree) {
            documentSearchService.indexDocument(item);
        }
    }
}
