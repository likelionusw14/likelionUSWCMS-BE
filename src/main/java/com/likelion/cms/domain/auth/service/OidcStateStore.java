package com.likelion.cms.domain.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    public void save(String state, String nonce, String frontendOrigin) {
        StateValue value = new StateValue(nonce, frontendOrigin);
        redisTemplate.opsForValue().set(key(state), serialize(value), TTL);
    }

    /**
     * Single-use: the state entry is deleted on read so a replayed
     * callback request (same state twice) cannot succeed.
     */
    public Optional<StateValue> consume(String state) {
        String json = redisTemplate.opsForValue().getAndDelete(key(state));
        return Optional.ofNullable(json).map(this::deserialize);
    }

    private String key(String state) {
        return KEY_PREFIX + state;
    }

    private String serialize(StateValue value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("OIDC state 직렬화에 실패했습니다.", e);
        }
    }

    private StateValue deserialize(String json) {
        try {
            return objectMapper.readValue(json, StateValue.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("OIDC state 역직렬화에 실패했습니다.", e);
        }
    }

    /**
     * frontendOrigin is null when the client didn't request a dynamic
     * redirect target (or the requested origin wasn't in the allowlist);
     * the caller falls back to the configured default in that case.
     */
    public record StateValue(String nonce, String frontendOrigin) {
    }
}
