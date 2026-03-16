package com.simpleblog.model.entity;

import lombok.Getter;

@Getter
public enum DocumentNodeType {
    FOLDER("folder"),
    DOC("doc");

    private final String value;

    DocumentNodeType(String value) {
        this.value = value;
    }

    public static DocumentNodeType fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (DocumentNodeType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown document node type: " + value);
    }
}
