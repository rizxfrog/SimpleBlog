package com.simpleblog.common.entity;

/**
 * Redis Key Specification
 * Key 规范：
 *  app：应用名（mytube / simpleblog）
 *  env：环境隔离（dev/test/prod）
 *  biz/sub：业务域+子域（user:profile / video:stat）
 *  id...：唯一标识（userId / videoId / date / tenantId）
 *  version：key 版本（结构变更时用 v2/v3，便于灰度与回收）
 */
public record RedisKeySpec(
        String biz,
        String sub,
        int version,
        boolean useHashTag
) {
    public static RedisKeySpec of(String biz, String sub) { return new RedisKeySpec(biz, sub, 1, false); }
    public RedisKeySpec v(int v) { return new RedisKeySpec(biz, sub, v, useHashTag); }
    public RedisKeySpec hashTag() { return new RedisKeySpec(biz, sub, version, true); }
}
