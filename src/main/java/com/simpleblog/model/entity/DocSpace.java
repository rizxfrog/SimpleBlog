package com.simpleblog.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.OffsetDateTime;

@TableName("doc_space")
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class DocSpace {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    @TableField("create_at")
    private OffsetDateTime createAt;

    @TableField("update_at")
    private OffsetDateTime updateAt;

    @TableField("delete_at")
    private OffsetDateTime deleteAt;

    @TableField("is_deleted")
    private Boolean isDeleted;

    @TableField("owner_id")
    private Long ownerId;
}
