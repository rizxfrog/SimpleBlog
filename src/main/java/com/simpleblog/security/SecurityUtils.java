package com.simpleblog.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;
import java.util.Optional;

public final class SecurityUtils {
    private SecurityUtils() {
    }

    public static Optional<String> currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        return Optional.ofNullable(authentication.getName());
    }
    public static Optional<Long> currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object details = authentication.getDetails();
        if (details instanceof Map<?, ?> detailMap) {
            Long userId = parseLongValue(detailMap.get("user_id"));
            if (userId != null) {
                // 一般是返回这个
                return Optional.of(userId);
            }
        }
        Long principalId = parseLongValue(authentication.getPrincipal());
        if (principalId != null) {
            return Optional.of(principalId);
        }
        return Optional.ofNullable(parseLongValue(authentication.getName()));
    }

    private static Long parseLongValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
