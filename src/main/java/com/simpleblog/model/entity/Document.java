package com.simpleblog.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.simpleblog.mapper.typehandler.DocumentNodeTypeTypeHandler;
import com.simpleblog.mapper.typehandler.LtreeTypeHandler;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@TableName("documents")
public class Document {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("parent_id")
    private Long parentId;

    @TableField(value = "type", typeHandler = DocumentNodeTypeTypeHandler.class)
    private DocumentNodeType type;

    private String title;

    private String content;

    @TableField("doc_project")
    private String project;

    @TableField("doc_version")
    private String version;

    @TableField(value = "path", typeHandler = LtreeTypeHandler.class)
    private String path;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("is_hidden")
    private Boolean hidden;

    @TableField("created_by")
    private Long createdBy;

    @TableField("updated_by")
    private Long updatedBy;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("depth")
    private Integer depth;

    @TableField(exist = false)
    private List<Document> children = new ArrayList<>();

    @TableField(exist = false)
    private String slug;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public DocumentNodeType getType() {
        return type;
    }

    public void setType(DocumentNodeType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getProject() {
        if (project == null || project.isBlank()) {
            return "default";
        }
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getVersion() {
        if (version == null || version.isBlank()) {
            return "default";
        }
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getSortOrder() {
        return sortOrder == null ? 0 : sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getHidden() {
        return hidden != null && hidden;
    }

    public void setHidden(Boolean hidden) {
        this.hidden = hidden;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getDepth() {
        return depth;
    }

    public void setDepth(Integer depth) {
        this.depth = depth;
    }

    public List<Document> getChildren() {
        return children;
    }

    public void setChildren(List<Document> children) {
        this.children = children;
    }

    public String getSlug() {
        if (slug != null) {
            return slug;
        }
        if (path == null) {
            return null;
        }
        int idx = path.lastIndexOf('.');
        return idx >= 0 ? path.substring(idx + 1) : path;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }
}

