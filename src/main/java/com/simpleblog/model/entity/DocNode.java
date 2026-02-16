package com.simpleblog.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.simpleblog.mapper.typehandler.DocumentNodeTypeTypeHandler;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@TableName("doc_node")
public class DocNode {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("space_id")
    private Long spaceId;

    @TableField("parent_id")
    private Long parentId;

    @TableField(value = "node_type", typeHandler = DocumentNodeTypeTypeHandler.class)
    private DocumentNodeType nodeType;

    private String title;

    @TableField("sort_key")
    private Integer sortKey;

    @TableField("is_deleted")
    private Boolean deleted;

    @TableField("create_at")
    private OffsetDateTime createAt;

    @TableField("update_at")
    private OffsetDateTime updateAt;

    @TableField(exist = false)
    private Long docId;

    @TableField(exist = false)
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

    public OffsetDateTime getCreateAt() {
        return createAt;
    }

    public void setCreateAt(OffsetDateTime createAt) {
        this.createAt = createAt;
    }

    public OffsetDateTime getUpdateAt() {
        return updateAt;
    }

    public void setUpdateAt(OffsetDateTime updateAt) {
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
