package com.simpleblog.common.utils;

public class TextCipher {
    public static String encrypt(String text, String key) throws Exception {
        byte[] encrypted =
                AESUtil.encrypt(text.getBytes("UTF-8"), key);
        return ChineseBase4096.encode(encrypted);
    }

    public static String decrypt(String cipher, String key) throws Exception {
        byte[] decoded =
                ChineseBase4096.decode(cipher);
        byte[] plain =
                AESUtil.decrypt(decoded, key);
        return new String(plain, "UTF-8");
    }
}