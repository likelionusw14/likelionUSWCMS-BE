package com.likelion.cms.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Carries a verified Kakao subject between the OIDC callback and account
 * creation, without ever creating a placeholder AppUser row. The cookie only
 * ever holds an opaque sessionId -- never the raw kakaoSubject.
 */
@Component
@RequiredArgsConstructor
public class OnboardingSessionStore {

    private static final String KEY_PREFIX = "auth:onboarding:";
    private static final Duration TTL = Duration.ofMinutes(20);

    private final StringRedisTemplate redisTemplate;

    public String create(String kakaoSubject) {
        String sessionId = OpaqueTokenGenerator.generate();
        redisTemplate.opsForValue().set(key(sessionId), kakaoSubject, TTL);
        return sessionId;
    }

    /**
     * Does not delete on read: a failed POST /accounts (e.g. duplicate
     * studentId) must let the user retry with the same session before it
     * expires.
     */
    public Optional<String> resolve(String sessionId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key(sessionId)));
    }

    public void consume(String sessionId) {
        redisTemplate.delete(key(sessionId));
    }

    private String key(String sessionId) {
        return KEY_PREFIX + sessionId;
    }
}
