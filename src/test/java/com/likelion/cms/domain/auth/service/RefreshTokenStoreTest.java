package com.likelion.cms.domain.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenStoreTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RefreshTokenStore store;

    @BeforeEach
    void setUp() {
        store = new RefreshTokenStore(redisTemplate);
    }

    @Test
    void issueStoresHashedTokenKeyedToUserIdWithFourteenDayTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String rawToken = store.issue(42L);

        assertThat(rawToken).isNotBlank();
        verify(valueOperations).set(anyString(), eq("42"), eq(Duration.ofDays(14)));
    }

    @Test
    void resolveLooksUpByHashOfRawToken() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        String rawToken = store.issue(42L);
        verify(valueOperations).set(keyCaptor.capture(), eq("42"), eq(Duration.ofDays(14)));
        when(valueOperations.get(keyCaptor.getValue())).thenReturn("42");

        Optional<Long> resolved = store.resolve(rawToken);

        assertThat(resolved).contains(42L);
    }

    @Test
    void resolveReturnsEmptyWhenTokenUnknown() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        assertThat(store.resolve("unknown-token")).isEmpty();
    }

    @Test
    void revokeDeletesTheHashedKey() {
        store.revoke("some-raw-token");

        verify(redisTemplate).delete(anyString());
    }
}
