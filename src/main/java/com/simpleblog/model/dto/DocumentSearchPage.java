package com.simpleblog.model.dto;

import com.simpleblog.model.entity.Document;

import java.util.List;

public record DocumentSearchPage(List<Document> items,
                                 long total,
                                 int page,
                                 int size,
                                 String query) {
}
