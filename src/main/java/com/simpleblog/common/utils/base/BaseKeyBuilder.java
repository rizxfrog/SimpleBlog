package com.simpleblog.common.utils.base;

import com.simpleblog.common.utils.RedisKeySpec;

public class BaseKeyBuilder {
    protected static final String app = "simple_blog";
    protected static final String env = "dev";     // dev/test/prod
    protected static final String tenant = "default";  // 可选：没有就传 null 或 "default"

    static String key(RedisKeySpec spec, Object... parts){
        return "";
    }
    /** 方便 scan：返回业务前缀，如 app:env:video:stat:* */
    static String prefix(RedisKeySpec spec){
        return "";
    }
}
