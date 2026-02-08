package com.simpleblog.model.dto;

import com.simpleblog.model.entity.DocumentNodeType;

public record DocumentUpdateInput(String title,
                                  String content,
                                  Integer sortOrder,
                                  Boolean hidden,
                                  String slug,
                                  DocumentNodeType type) {
}
