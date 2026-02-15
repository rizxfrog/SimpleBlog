package com.simpleblog.model.dto;

import com.simpleblog.model.entity.DocumentNodeType;

public record DocNodeCreateInput(Long spaceId,
                                 Long parentId,
                                 DocumentNodeType nodeType,
                                 String title,
                                 Integer sortKey,
                                 String contentMd) {
}
