package com.blog.cadenza;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

/**
 * 音乐平台令牌。博客只保存根密钥。
 * 明文为 {@code 平台id|过期秒}，AES-256-GCM 后形如 {@code v1.} + Base64URL(IV + 密文)。
 * 平台 id 从 {@link #MIN_PLATFORM_ID} 起。
 */
public final class CadenzaTokens {
    public static final int MIN_PLATFORM_ID = 32;

    private static final String PREFIX = "v1.";
    private static final int IV_LEN = 12;
    private static final int TAG_BITS = 128;
    private static final String TRANSFORM = "AES/GCM/NoPadding";
    private static final byte[] AAD = "cadenza-v1".getBytes(StandardCharsets.US_ASCII);
    private static final SecureRandom RANDOM = new SecureRandom();

    private CadenzaTokens() {
    }

    public static String issue(byte[] rootKey, int platformId, long expireEpochSeconds) {
        if (rootKey == null || rootKey.length != 32) {
            throw new IllegalArgumentException("根密钥无效");
        }
        if (platformId < MIN_PLATFORM_ID) {
            throw new IllegalArgumentException("平台 id 从 32 开始");
        }
        if (expireEpochSeconds <= 0) {
            throw new IllegalArgumentException("过期时间无效");
        }
        byte[] iv = new byte[IV_LEN];
        RANDOM.nextBytes(iv);
        byte[] plain = (platformId + "|" + expireEpochSeconds).getBytes(StandardCharsets.US_ASCII);
        byte[] encrypted = transform(Cipher.ENCRYPT_MODE, rootKey, iv, plain);
        byte[] packed = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, packed, 0, iv.length);
        System.arraycopy(encrypted, 0, packed, iv.length, encrypted.length);
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(packed);
    }

    public static int verify(byte[] rootKey, String token, Set<Integer> revoked, long nowEpochSeconds) {
        if (rootKey == null || rootKey.length != 32 || token == null || !token.startsWith(PREFIX)) {
            throw new IllegalArgumentException("令牌无效");
        }
        byte[] packed;
        try {
            packed = Base64.getUrlDecoder().decode(token.substring(PREFIX.length()));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("令牌无效");
        }
        if (packed.length <= IV_LEN) {
            throw new IllegalArgumentException("令牌无效");
        }
        byte[] iv = new byte[IV_LEN];
        byte[] encrypted = new byte[packed.length - IV_LEN];
        System.arraycopy(packed, 0, iv, 0, IV_LEN);
        System.arraycopy(packed, IV_LEN, encrypted, 0, encrypted.length);
        byte[] plain;
        try {
            plain = transform(Cipher.DECRYPT_MODE, rootKey, iv, encrypted);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("令牌无效");
        }
        String text = new String(plain, StandardCharsets.US_ASCII);
        int bar = text.indexOf('|');
        if (bar <= 0 || bar != text.lastIndexOf('|')) {
            throw new IllegalArgumentException("令牌无效");
        }
        int platformId;
        long expireEpochSeconds;
        try {
            platformId = Integer.parseInt(text.substring(0, bar));
            expireEpochSeconds = Long.parseLong(text.substring(bar + 1));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("令牌无效");
        }
        if (platformId < MIN_PLATFORM_ID || nowEpochSeconds >= expireEpochSeconds) {
            throw new IllegalArgumentException("令牌无效");
        }
        if (revoked != null && revoked.contains(platformId)) {
            throw new IllegalArgumentException("令牌无效");
        }
        return platformId;
    }

    public static byte[] decodeRootKey(String text) {
        if (text == null || text.isBlank()) {
            return new byte[0];
        }
        try {
            byte[] key = Base64.getDecoder().decode(text.trim());
            if (key.length != 32) {
                return new byte[0];
            }
            return key;
        } catch (IllegalArgumentException ex) {
            return new byte[0];
        }
    }

    public static Set<Integer> parseRevoked(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        Set<Integer> ids = new HashSet<>();
        for (String part : text.split(",")) {
            String item = part.trim();
            if (item.isEmpty()) {
                continue;
            }
            try {
                ids.add(Integer.parseInt(item));
            } catch (NumberFormatException ignored) {
                // 跳过写错的项，避免吊销名单笔误拖垮启动
            }
        }
        return Set.copyOf(ids);
    }

    private static byte[] transform(int mode, byte[] rootKey, byte[] iv, byte[] input) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(mode, new SecretKeySpec(rootKey, "AES"), new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(AAD);
            return cipher.doFinal(input);
        } catch (GeneralSecurityException ex) {
            throw new IllegalArgumentException("令牌无效");
        }
    }
}
