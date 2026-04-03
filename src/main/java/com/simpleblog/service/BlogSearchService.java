package com.simpleblog.service;

import com.simpleblog.model.entity.Blog;

public interface BlogSearchService {
    /**
     * 索引博客文档
     * @param blog 博客对象
     */
    void indexBlog(Blog blog);

    /**
     * 删除博客文档索引
     * @param blogId 博客ID
     */
    void deleteBlog(Long blogId);

    /**
     * 搜索博客
     * @param query 搜索关键词
     * @param page 页码
     * @param size 每页大小
     * @return 搜索结果
     */
    SearchResult search(String query, int page, int size);

    record SearchResult(java.util.List<Blog> items, long total) {}
}
