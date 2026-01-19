package com.simpleblog.model.dto;

public record CommentInput(Long blogId, Long parentId, String content) {
}
