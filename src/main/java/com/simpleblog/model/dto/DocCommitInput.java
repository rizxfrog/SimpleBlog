package com.simpleblog.model.dto;

public record DocCommitInput(Long docId,
                             String refName,
                             Long baseCommitId,
                             String title,
                             String contentMd,
                             String message) {
}
