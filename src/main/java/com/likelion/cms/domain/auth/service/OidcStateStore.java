package com.likelion.cms.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OidcStateStore {

    private static final String KEY_PREFIX = "auth:oidc:state:";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;

    public void save(String state, String nonce) {
        redisTemplate.opsForValue().set(key(state), nonce, TTL);
    }

    /**
     * Single-use: the state/nonce pair is deleted on read so a replayed
     * callback request (same state twice) cannot succeed.
     */
    public Optional<String> consumeNonce(String state) {
        return Optional.ofNullable(redisTemplate.opsForValue().getAndDelete(key(state)));
    }

    private String key(String state) {
        return KEY_PREFIX + state;
    }
}
