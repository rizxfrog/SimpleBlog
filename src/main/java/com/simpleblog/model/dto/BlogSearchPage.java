package com.simpleblog.model.dto;

import com.simpleblog.model.entity.Blog;

import java.util.List;

public record BlogSearchPage(
        List<Blog> items,
        long total,
        int page,
        int size,
        String query
) {
}

