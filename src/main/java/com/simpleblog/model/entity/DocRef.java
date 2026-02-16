package com.simpleblog.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.simpleblog.mapper.typehandler.DocRefTypeTypeHandler;

import java.time.OffsetDateTime;

@TableName("doc_ref")
public class DocRef {
    @TableField("doc_id")
    private Long docId;

    @TableId(value = "ref_name", type = IdType.INPUT)
    private String refName;

    @TableField("commit_id")
    private Long commitId;

    @TableField(value = "ref_type", typeHandler = DocRefTypeTypeHandler.class)
    private DocRefType refType;

    @TableField("update_at")
    private OffsetDateTime updateAt;

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

    public OffsetDateTime getUpdateAt() {
        return updateAt;
    }

    public void setUpdateAt(OffsetDateTime updateAt) {
        this.updateAt = updateAt;
    }
}
