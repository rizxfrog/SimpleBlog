package com.simpleblog.service;

import com.simpleblog.model.dto.DocumentSearchPage;
import com.simpleblog.model.entity.Document;

public interface DocumentSearchService {
    /**
     * 索引文档
     * @param document 文档对象
     */
    void indexDocument(Document document);

    /**
     * 删除文档索引
     * @param documentId 文档ID
     */
    void deleteDocument(Long documentId);

    /**
     * 搜索文档
     * @param query 搜索关键词
     * @param page 页码
     * @param size 每页大小
     * @param includeHidden 是否包含隐藏文档
     * @param project 项目名称
     * @param version 版本名称
     * @return 文档分页结果
     */
    DocumentSearchPage search(String query, int page, int size, boolean includeHidden, String project, String version);
}
