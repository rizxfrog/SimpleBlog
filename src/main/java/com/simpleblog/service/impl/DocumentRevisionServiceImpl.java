package com.simpleblog.service.impl;

import com.simpleblog.mapper.DocumentMapper;
import com.simpleblog.mapper.DocumentRevisionMapper;
import com.simpleblog.model.entity.Document;
import com.simpleblog.model.entity.DocumentNodeType;
import com.simpleblog.model.entity.DocumentRevision;
import com.simpleblog.service.DocumentRevisionService;
import com.simpleblog.service.DocumentSearchService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DocumentRevisionServiceImpl implements DocumentRevisionService {
    private final DocumentRevisionMapper documentRevisionMapper;
    private final DocumentMapper documentMapper;
    private final DocumentSearchService documentSearchService;

    public DocumentRevisionServiceImpl(DocumentRevisionMapper documentRevisionMapper,
                                       DocumentMapper documentMapper,
                                       DocumentSearchService documentSearchService) {
        this.documentRevisionMapper = documentRevisionMapper;
        this.documentMapper = documentMapper;
        this.documentSearchService = documentSearchService;
    }

    @Override
    public List<DocumentRevision> listByDocumentId(Long documentId) {
        if (documentId == null) {
            return List.of();
        }
        return documentRevisionMapper.listByDocumentId(documentId);
    }

    @Override
    public DocumentRevision findById(Long revisionId) {
        if (revisionId == null) {
            return null;
        }
        return documentRevisionMapper.selectById(revisionId);
    }

    @Transactional
    @Override
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
        document.setProject(revision.getProject());
        document.setVersion(revision.getVersion());

        Long parentId = revision.getParentId();
        String path = revision.getPath();
        if (parentId != null) {
            Document parent = documentMapper.selectById(parentId);
            if (parent == null
                    || !document.getProject().equals(parent.getProject())
                    || !document.getVersion().equals(parent.getVersion())) {
                parentId = null;
                path = normalizeRootPath(path);
            }
        }

        String project = document.getProject();
        String version = document.getVersion();
        String uniquePath = ensureUniquePath(path, document.getId(), project, version);
        if (oldPath != null && !oldPath.equals(uniquePath)) {
            documentMapper.updatePathPrefix(oldPath, uniquePath, project, version);
        }
        document.setParentId(parentId);
        document.setPath(uniquePath);
        document.setUpdatedBy(userId);
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);
        reindexSubtree(uniquePath, project, version);
        return document;
    }

    @Override
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
        revision.setProject(document.getProject());
        revision.setVersion(document.getVersion());
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

    private String ensureUniquePath(String basePath, Long documentId, String project, String version) {
        if (basePath == null || basePath.isBlank()) {
            basePath = "restored";
        }
        if (documentMapper.countByPathExcludingId(basePath, documentId, project, version) == 0) {
            return basePath;
        }
        String candidate = basePath;
        int suffix = 2;
        while (documentMapper.countByPathExcludingId(candidate, documentId, project, version) > 0) {
            candidate = basePath + "_r" + suffix;
            suffix += 1;
        }
        return candidate;
    }

    private void reindexSubtree(String rootPath, String project, String version) {
        if (rootPath == null || rootPath.isBlank()) {
            return;
        }
        List<Document> subtree = documentMapper.listSubtree(rootPath, true, project, version);
        for (Document item : subtree) {
            documentSearchService.indexDocument(item);
        }
    }
}
