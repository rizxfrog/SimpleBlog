package com.simpleblog.model.dto;

public record DocNodeUpdateInput(String title,
                                 Integer sortKey,
                                 Boolean deleted) {
}
