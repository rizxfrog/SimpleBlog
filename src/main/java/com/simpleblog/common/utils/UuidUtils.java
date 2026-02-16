package com.simpleblog.common.utils;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

public class UuidUtils {
    public static String generateShortUuid() {
        UUID uuid = UUID.randomUUID();
        byte[] bytes = toBytes(uuid);
        // 使用 URL 安全的 Base64 编码，去掉填充字符 '='
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static byte[] toBytes(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }
}
