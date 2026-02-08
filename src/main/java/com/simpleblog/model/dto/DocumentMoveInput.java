package com.simpleblog.model.dto;

public record DocumentMoveInput(Long parentId,
                                Integer sortOrder,
                                String slug) {
}
