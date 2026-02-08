package com.simpleblog.service;

import com.simpleblog.mapper.DocumentMapper;
import com.simpleblog.mapper.DocumentRevisionMapper;
import com.simpleblog.model.entity.Document;
import com.simpleblog.model.entity.DocumentNodeType;
import com.simpleblog.model.entity.DocumentRevision;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DocumentRevisionService {
    private final DocumentRevisionMapper documentRevisionMapper;
    private final DocumentMapper documentMapper;
    private final DocumentSearchService documentSearchService;

    public DocumentRevisionService(DocumentRevisionMapper documentRevisionMapper,
                                   DocumentMapper documentMapper,
                                   DocumentSearchService documentSearchService) {
        this.documentRevisionMapper = documentRevisionMapper;
        this.documentMapper = documentMapper;
        this.documentSearchService = documentSearchService;
    }

    public List<DocumentRevision> listByDocumentId(Long documentId) {
        if (documentId == null) {
            return List.of();
        }
        return documentRevisionMapper.listByDocumentId(documentId);
    }

    public DocumentRevision findById(Long revisionId) {
        if (revisionId == null) {
            return null;
        }
        return documentRevisionMapper.selectById(revisionId);
    }

    @Transactional
    public Document restore(Long revisionId, Long userId) {
        DocumentRevision revision = documentRevisionMapper.selectById(revisionId);
        if (revision == null) {
            throw new IllegalArgumentException("Revision not found: " + revisionId);
        }
        Document document = documentMapper.selectById(revision.getDocumentId());
        if (document == null) {
            throw new IllegalArgumentException("Document not found: " + revision.getDocumentId());
        }
        recordRevision(document, userId);

        DocumentNodeType type = revision.getType();
        String oldPath = document.getPath();
        document.setTitle(revision.getTitle());
        document.setContent(type == DocumentNodeType.DOC ? revision.getContent() : null);
        document.setType(type);
        document.setHidden(revision.getHidden());
        document.setSortOrder(revision.getSortOrder());

        Long parentId = revision.getParentId();
        String path = revision.getPath();
        if (parentId != null && documentMapper.selectById(parentId) == null) {
            parentId = null;
            path = normalizeRootPath(path);
        }

        String uniquePath = ensureUniquePath(path, document.getId());
        if (oldPath != null && !oldPath.equals(uniquePath)) {
            documentMapper.updatePathPrefix(oldPath, uniquePath);
        }
        document.setParentId(parentId);
        document.setPath(uniquePath);
        document.setUpdatedBy(userId);
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);
        reindexSubtree(uniquePath);
        return document;
    }

    public void recordRevision(Document document, Long userId) {
        if (document == null || document.getId() == null) {
            return;
        }
        int next = documentRevisionMapper.maxRevisionNumber(document.getId()) + 1;
        DocumentRevision revision = new DocumentRevision();
        revision.setDocumentId(document.getId());
        revision.setType(document.getType());
        revision.setTitle(document.getTitle());
        revision.setContent(document.getContent());
        revision.setPath(document.getPath());
        revision.setParentId(document.getParentId());
        revision.setSortOrder(document.getSortOrder());
        revision.setHidden(document.getHidden());
        revision.setRevisionNumber(next);
        revision.setCreatedBy(userId);
        revision.setCreatedAt(LocalDateTime.now());
        documentRevisionMapper.insert(revision);
    }

    private String normalizeRootPath(String path) {
        if (path == null || path.isBlank()) {
            return "restored";
        }
        String slug = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
        return slug.isBlank() ? "restored" : slug;
    }

    private String ensureUniquePath(String basePath, Long documentId) {
        if (basePath == null || basePath.isBlank()) {
            basePath = "restored";
        }
        if (documentMapper.countByPathExcludingId(basePath, documentId) == 0) {
            return basePath;
        }
        String candidate = basePath;
        int suffix = 2;
        while (documentMapper.countByPathExcludingId(candidate, documentId) > 0) {
            candidate = basePath + "_r" + suffix;
            suffix += 1;
        }
        return candidate;
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
