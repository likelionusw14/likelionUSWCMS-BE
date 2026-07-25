package com.likelion.cms.domain.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnboardingSessionStoreTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private OnboardingSessionStore store;

    @BeforeEach
    void setUp() {
        store = new OnboardingSessionStore(redisTemplate);
    }

    @Test
    void createStoresKakaoSubjectKeyedByOpaqueSessionId() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String sessionId = store.create("kakao-sub-123");

        assertThat(sessionId).isNotBlank();
        verify(valueOperations).set(eq("auth:onboarding:" + sessionId), eq("kakao-sub-123"), eq(Duration.ofMinutes(20)));
    }

    @Test
    void resolveDoesNotDeleteOnRead() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:onboarding:session-1")).thenReturn("kakao-sub-123");

        Optional<String> resolved = store.resolve("session-1");

        assertThat(resolved).contains("kakao-sub-123");
        verify(valueOperations, never()).getAndDelete(anyString());
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void resolveReturnsEmptyWhenSessionUnknown() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        assertThat(store.resolve("unknown-session")).isEmpty();
    }

    @Test
    void consumeDeletesTheSession() {
        store.consume("session-1");

        verify(redisTemplate).delete("auth:onboarding:session-1");
    }
}
