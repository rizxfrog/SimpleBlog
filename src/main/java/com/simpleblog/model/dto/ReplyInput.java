package com.simpleblog.model.dto;

public record ReplyInput(
        Long commentId,
        String content,
        String authorName,
        String authorEmail,
        String authorWebsite
) {
}
