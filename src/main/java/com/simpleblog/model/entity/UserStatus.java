package com.simpleblog.model.entity;

public enum UserStatus {
    ACTIVE("active"),
    DISABLED("disabled"),
    PENDING("pending");

    private final String value;

    UserStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static UserStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (UserStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown user status: " + value);
    }
}
