package com.simpleblog.service;

public interface CommentRateLimiter {
    /**
     * 检查是否允许评论
     * @param key 限流键(通常是IP)
     * @param limit 限制次数
     * @param windowMs 时间窗口(毫秒)
     * @return 是否允许
     */
    boolean allow(String key, int limit, long windowMs);
}
