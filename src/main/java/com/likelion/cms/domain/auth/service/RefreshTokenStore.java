package com.likelion.cms.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Stores refresh tokens keyed by a hash of the raw token value, not the raw
 * value itself, so a Redis dump/leak doesn't hand out usable session tokens.
 * No userId -> token index is kept on purpose: multiple concurrent sessions
 * (different devices/browsers) are allowed, matching the product decision to
 * not invalidate other sessions on a new login.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "auth:refresh:";
    private static final Duration TTL = Duration.ofDays(14);

    private final StringRedisTemplate redisTemplate;

    public String issue(Long userId) {
        String rawToken = OpaqueTokenGenerator.generate();
        redisTemplate.opsForValue().set(key(rawToken), String.valueOf(userId), TTL);
        return rawToken;
    }

    /**
     * Atomically reads and deletes the token in one Redis round trip (GETDEL),
     * so two concurrent redemptions of the same raw token can't both succeed --
     * only the first caller gets a userId back, the second sees it already gone.
     * A malformed stored value (shouldn't happen in practice) is treated as an
     * invalid token rather than propagating a parse error.
     */
    public Optional<Long> consume(String rawToken) {
        String value = redisTemplate.opsForValue().getAndDelete(key(rawToken));
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.valueOf(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public void revoke(String rawToken) {
        redisTemplate.delete(key(rawToken));
    }

    private String key(String rawToken) {
        return KEY_PREFIX + sha256Hex(rawToken);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
