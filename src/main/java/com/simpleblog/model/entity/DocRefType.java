package com.simpleblog.model.entity;

public enum DocRefType {
    BRANCH("branch"),
    TAG("tag");

    private final String value;

    DocRefType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static DocRefType fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (DocRefType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown doc ref type: " + value);
    }
}
