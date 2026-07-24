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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OidcStateStoreTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private OidcStateStore store;

    @BeforeEach
    void setUp() {
        store = new OidcStateStore(redisTemplate);
    }

    @Test
    void saveStoresNonceKeyedByState() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        store.save("state-1", "nonce-1");

        verify(valueOperations).set("auth:oidc:state:state-1", "nonce-1", Duration.ofMinutes(10));
    }

    @Test
    void consumeNonceReturnsAndDeletesInOneShot() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete("auth:oidc:state:state-1")).thenReturn("nonce-1");

        Optional<String> nonce = store.consumeNonce("state-1");

        assertThat(nonce).contains("nonce-1");
    }

    @Test
    void consumeNonceReturnsEmptyWhenStateUnknown() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete(anyString())).thenReturn(null);

        assertThat(store.consumeNonce("unknown")).isEmpty();
    }
}
