package com.simpleblog.common.utils;

public final class RedisKeyBuilder {
    private static final String PREFIX = "simpleblog";

    private RedisKeyBuilder() {
    }

    public static String userByUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username is required");
        }
        return PREFIX + ":user:username:" + username;
    }

    public static String userIdByUsername(String username) {
        return userByUsername(username) + ":id";
    }
}
