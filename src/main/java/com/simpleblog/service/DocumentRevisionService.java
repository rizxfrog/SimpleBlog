package com.simpleblog.service;

import com.simpleblog.model.entity.Document;
import com.simpleblog.model.entity.DocumentRevision;

import java.util.List;

public interface DocumentRevisionService {
    /**
     * 查询文档的所有修订版本
     * @param documentId 文档ID
     * @return 修订版本列表
     */
    List<DocumentRevision> listByDocumentId(Long documentId);

    /**
     * 根据ID查找修订版本
     * @param revisionId 修订版本ID
     * @return 修订版本对象
     */
    DocumentRevision findById(Long revisionId);

    /**
     * 恢复文档到指定修订版本
     * @param revisionId 修订版本ID
     * @param userId 用户ID
     * @return 恢复后的文档
     */
    Document restore(Long revisionId, Long userId);

    /**
     * 记录文档修订
     * @param document 文档对象
     * @param userId 用户ID
     */
    void recordRevision(Document document, Long userId);
}
