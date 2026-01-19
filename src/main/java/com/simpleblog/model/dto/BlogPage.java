package com.simpleblog.model.dto;

import com.simpleblog.model.entity.Blog;

import java.util.List;

public record BlogPage(List<Blog> items, long total, int page, int size) {
}
