package com.simpleblog.common.utils;

import lombok.NonNull;

public final class RedisKeyQuickBuilder {
    private static final String PREFIX = "simpleblog";
    public static final Spaces spaces = new Spaces();

    private RedisKeyQuickBuilder() {
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

    public static String docTree(Long spaceId, boolean includeDeleted) {
        if (spaceId == null) {
            throw new IllegalArgumentException("spaceId is required");
        }
        return PREFIX + ":doc:tree:space:" + spaceId + ":include_deleted:" + includeDeleted;
    }

    public static String spaceExists(@NonNull Long spaceId) {
        return PREFIX + ":doc:tree:space:exist:" + spaceId;
    }

    public static String docTreePattern(Long spaceId) {
        if (spaceId == null) {
            throw new IllegalArgumentException("spaceId is required");
        }
        return PREFIX + ":doc:tree:space:" + spaceId + ":*";
    }

    public static String quickBuild(String key, String id) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key is required");
        }
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id is required");
        }
//        String normalizedKey = trimColon(key);
//        if (normalizedKey.isEmpty()) {
//            throw new IllegalArgumentException("key is required");
//        }
//        String suffix = endsWithIdSegment(normalizedKey) ? ":" + id : ":id:" + id;
//        return PREFIX + ":" + normalizedKey + suffix;
        return PREFIX + ":" + key + ":" + id;
    }

    public static String quickBuild(KeyPath key, String id) {
        if (key == null) {
            throw new IllegalArgumentException("key is required");
        }
        return quickBuild(key.path, id);
    }

    private static String trimColon(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && value.charAt(start) == ':') {
            start++;
        }
        while (end > start && value.charAt(end - 1) == ':') {
            end--;
        }
        return value.substring(start, end);
    }

    private static boolean endsWithIdSegment(String key) {
        return "id".equals(key) || key.endsWith(":id");
    }

    public static final class Spaces {
        public final SystemSpace docSystem = new SystemSpace("doc");
        public final SystemSpace articleSystem = new SystemSpace("article");

        private Spaces() {
        }
    }

    public static class KeyPath {
        public final String name;
        public final String path;

        protected KeyPath(String name, String path) {
            this.name = name;
            this.path = path;
        }

        @Override
        public String toString() {
            return path;
        }
    }

    public static final class SystemSpace extends KeyPath {
        public final NodeSpace node;

        private SystemSpace(String name) {
            super(name, name);
            this.node = new NodeSpace(this);
        }
    }

    public static final class NodeSpace extends KeyPath {
        public final KeyPath id;

        private NodeSpace(KeyPath parent) {
            super("node", parent.path + ":node");
            this.id = new KeyPath("id", path + ":id");
        }
    }

}
