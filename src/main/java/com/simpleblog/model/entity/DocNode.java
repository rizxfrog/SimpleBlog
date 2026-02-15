package com.simpleblog.model.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DocNode {
    private Long id;
    private Long spaceId;
    private Long parentId;
    private DocumentNodeType nodeType;
    private String title;
    private Integer sortKey;
    private Boolean deleted;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;
    private Long docId;
    private List<DocNode> children = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(Long spaceId) {
        this.spaceId = spaceId;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public DocumentNodeType getNodeType() {
        return nodeType;
    }

    public void setNodeType(DocumentNodeType nodeType) {
        this.nodeType = nodeType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getSortKey() {
        return sortKey == null ? 0 : sortKey;
    }

    public void setSortKey(Integer sortKey) {
        this.sortKey = sortKey;
    }

    public Boolean getDeleted() {
        return deleted != null && deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public LocalDateTime getCreateAt() {
        return createAt;
    }

    public void setCreateAt(LocalDateTime createAt) {
        this.createAt = createAt;
    }

    public LocalDateTime getUpdateAt() {
        return updateAt;
    }

    public void setUpdateAt(LocalDateTime updateAt) {
        this.updateAt = updateAt;
    }

    public Long getDocId() {
        return docId;
    }

    public void setDocId(Long docId) {
        this.docId = docId;
    }

    public List<DocNode> getChildren() {
        return children;
    }

    public void setChildren(List<DocNode> children) {
        this.children = children;
    }
}
