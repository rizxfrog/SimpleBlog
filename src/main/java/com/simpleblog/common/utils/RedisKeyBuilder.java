package com.simpleblog.common.utils;

import com.simpleblog.common.utils.base.BaseKeyBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.StringJoiner;

public class RedisKeyBuilder extends BaseKeyBuilder  {
    public static String key(RedisKeySpec spec, Object... parts) {
        Objects.requireNonNull(spec, "spec");

        StringJoiner sj = new StringJoiner(":");
        sj.add(app).add(env);
        sj.add(tenant);

        sj.add(requireSafe(spec.biz(), "biz"));
        sj.add(requireSafe(spec.sub(), "sub"));

        // parts
        if (parts != null) {
            for (Object p : parts) {
                sj.add(encodePart(p));
            }
        }

        // 版本尾缀（也可以放在 biz/sub 后面，看你团队习惯）
        sj.add("v" + spec.version());

        return sj.toString();
    }

    public static String prefix(RedisKeySpec spec) {
        // 用于 scan：prefix + ":*"
        return key(spec, "*");
    }

    /** 对 part 做编码：避免冒号、空格等破坏层级；也可选择强校验只允许 [a-zA-Z0-9_-] */
    static String encodePart(Object part) {
        if (part == null) return "null";
        String s = String.valueOf(part);

        // Cluster hash tag：如果 part 里已经含 {...}，这里就不再包；否则你可以把“tag”作为第一个 part 传入
        // 这里仅做编码，不强行插入 hash tag（建议你用一个显式方法来构建带 tag 的 key）
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    static String requireSafe(String s, String name) {
        Objects.requireNonNull(s, name + " is null");
        if (s.isBlank()) throw new IllegalArgumentException(name + " is blank");
        if (s.contains(":")) throw new IllegalArgumentException(name + " must not contain ':'");
        return s;
    }
}
