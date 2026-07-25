package com.likelion.cms.domain.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenStoreTest {

    private static final String KEY_PREFIX = "auth:refresh:";

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
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);

        String rawToken = store.issue(42L);

        assertThat(rawToken).isNotBlank();
        verify(valueOperations).set(keyCaptor.capture(), eq("42"), eq(Duration.ofDays(14)));
        assertThat(keyCaptor.getValue()).isEqualTo(expectedKey(rawToken));
    }

    @Test
    void consumeAtomicallyReadsAndDeletesByHashOfRawToken() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String rawToken = "some-raw-token";
        when(valueOperations.getAndDelete(expectedKey(rawToken))).thenReturn("42");

        Optional<Long> resolved = store.consume(rawToken);

        assertThat(resolved).contains(42L);
    }

    @Test
    void consumeReturnsEmptyWhenTokenUnknown() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete(anyString())).thenReturn(null);

        assertThat(store.consume("unknown-token")).isEmpty();
    }

    @Test
    void consumeReturnsEmptyWhenStoredValueIsNotNumeric() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String rawToken = "some-raw-token";
        when(valueOperations.getAndDelete(expectedKey(rawToken))).thenReturn("not-a-number");

        assertThat(store.consume(rawToken)).isEmpty();
    }

    @Test
    void revokeDeletesTheHashedKey() {
        String rawToken = "some-raw-token";

        store.revoke(rawToken);

        verify(redisTemplate).delete(expectedKey(rawToken));
    }

    private String expectedKey(String rawToken) {
        return KEY_PREFIX + sha256Hex(rawToken);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
