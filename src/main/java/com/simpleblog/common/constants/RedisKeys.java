package com.simpleblog.common.constants;

import com.simpleblog.common.utils.RedisKeySpec;

public class RedisKeys {
    private RedisKeys() {}

    private final static int VERSION = 1;

    // ===== 文档系统 =====
    public static final RedisKeySpec DOC_NODE = RedisKeySpec.of("doc", "node").v(VERSION);
    public static final RedisKeySpec DOC_NODE_NEXT_SORTKEY = RedisKeySpec.of("doc", "next_sortkey_of_spaceId_parentId").v(VERSION);

    // 用户资料缓存：mytube:prod:user:profile:{id}:v1
    public static final RedisKeySpec USER_PROFILE = RedisKeySpec.of("user", "profile").v(VERSION);

    // 同一用户相关 key 同槽（Cluster）：mytube:prod:im:session:{tenant:userId}:v1
    public static final RedisKeySpec IM_SESSION = RedisKeySpec.of("im", "session").v(VERSION).hashTag();
}
