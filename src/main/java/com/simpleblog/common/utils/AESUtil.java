package com.simpleblog.common.utils;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import java.security.spec.KeySpec;
import java.util.*;

public class AESUtil {

    private static final int KEY_SIZE = 256;
    private static final int IV_LENGTH = 12;

    public static byte[] encrypt(byte[] data, String password) throws Exception {

        byte[] iv = new byte[IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        SecretKey key = deriveKey(password);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);

        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

        byte[] encrypted = cipher.doFinal(data);

        byte[] result = new byte[iv.length + encrypted.length];
        System.arraycopy(iv,0,result,0,iv.length);
        System.arraycopy(encrypted,0,result,iv.length,encrypted.length);

        return result;
    }

    public static byte[] decrypt(byte[] data, String password) throws Exception {

        byte[] iv = Arrays.copyOfRange(data,0,12);
        byte[] cipherText = Arrays.copyOfRange(data,12,data.length);

        SecretKey key = deriveKey(password);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);

        cipher.init(Cipher.DECRYPT_MODE,key,spec);

        return cipher.doFinal(cipherText);
    }

    private static SecretKey deriveKey(String password) throws Exception {

        byte[] salt = "TextCipherSalt".getBytes();

        SecretKeyFactory factory =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        KeySpec spec =
                new PBEKeySpec(password.toCharArray(),salt,65536,KEY_SIZE);

        byte[] keyBytes = factory.generateSecret(spec).getEncoded();

        return new SecretKeySpec(keyBytes,"AES");
    }
}