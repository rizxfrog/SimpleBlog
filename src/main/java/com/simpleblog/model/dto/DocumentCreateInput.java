package com.simpleblog.model.dto;

import com.simpleblog.model.entity.DocumentNodeType;

public record DocumentCreateInput(String title,
                                  String content,
                                  Long parentId,
                                  Integer sortOrder,
                                  Boolean hidden,
                                  String slug,
                                  DocumentNodeType type) {
}
