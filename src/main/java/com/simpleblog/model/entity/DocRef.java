package com.simpleblog.model.entity;

import java.time.LocalDateTime;

public class DocRef {
    private Long docId;
    private String refName;
    private Long commitId;
    private DocRefType refType;
    private LocalDateTime updateAt;

    public Long getDocId() {
        return docId;
    }

    public void setDocId(Long docId) {
        this.docId = docId;
    }

    public String getRefName() {
        return refName;
    }

    public void setRefName(String refName) {
        this.refName = refName;
    }

    public Long getCommitId() {
        return commitId;
    }

    public void setCommitId(Long commitId) {
        this.commitId = commitId;
    }

    public DocRefType getRefType() {
        return refType;
    }

    public void setRefType(DocRefType refType) {
        this.refType = refType;
    }

    public LocalDateTime getUpdateAt() {
        return updateAt;
    }

    public void setUpdateAt(LocalDateTime updateAt) {
        this.updateAt = updateAt;
    }
}
