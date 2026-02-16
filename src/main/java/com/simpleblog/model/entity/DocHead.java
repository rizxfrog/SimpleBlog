package com.simpleblog.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.OffsetDateTime;

@TableName("doc_head")
public class DocHead {
    @TableId(value = "doc_id", type = IdType.INPUT)
    private Long docId;

    @TableField("head_ref")
    private String headRef;

    @TableField("ref_version")
    private Long refVersion;

    @TableField("update_at")
    private OffsetDateTime updateAt;

    public Long getDocId() {
        return docId;
    }

    public void setDocId(Long docId) {
        this.docId = docId;
    }

    public String getHeadRef() {
        return headRef;
    }

    public void setHeadRef(String headRef) {
        this.headRef = headRef;
    }

    public Long getRefVersion() {
        return refVersion;
    }

    public void setRefVersion(Long refVersion) {
        this.refVersion = refVersion;
    }

    public OffsetDateTime getUpdateAt() {
        return updateAt;
    }

    public void setUpdateAt(OffsetDateTime updateAt) {
        this.updateAt = updateAt;
    }
}
