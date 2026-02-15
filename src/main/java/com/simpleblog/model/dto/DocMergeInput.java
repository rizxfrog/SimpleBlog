package com.simpleblog.model.dto;

public record DocMergeInput(Long docId,
                            String targetRef,
                            String sourceRef,
                            Long targetBaseCommitId,
                            String title,
                            String contentMd,
                            String message) {
}
