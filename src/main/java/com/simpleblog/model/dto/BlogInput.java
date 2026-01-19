package com.simpleblog.model.dto;

import java.util.List;

public record BlogInput(String title,
                        String summary,
                        String content,
                        Long categoryId,
                        String coverUrl,
                        Boolean published,
                        List<Long> tagIds) {
}
