package com.likelion.cms.domain.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OidcStateStoreTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private OidcStateStore store;

    @BeforeEach
    void setUp() {
        store = new OidcStateStore(redisTemplate, objectMapper);
    }

    @Test
    void saveStoresNonceAndOriginKeyedByState() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        store.save("state-1", "nonce-1", "http://localhost:5173");

        verify(valueOperations).set(
                eq("auth:oidc:state:state-1"),
                eq("{\"nonce\":\"nonce-1\",\"frontendOrigin\":\"http://localhost:5173\"}"),
                eq(Duration.ofMinutes(10)));
    }

    @Test
    void saveAllowsNullOrigin() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        store.save("state-1", "nonce-1", null);

        verify(valueOperations).set(
                eq("auth:oidc:state:state-1"),
                eq("{\"nonce\":\"nonce-1\",\"frontendOrigin\":null}"),
                eq(Duration.ofMinutes(10)));
    }

    @Test
    void consumeReturnsAndDeletesInOneShot() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete("auth:oidc:state:state-1"))
                .thenReturn("{\"nonce\":\"nonce-1\",\"frontendOrigin\":\"http://localhost:5173\"}");

        Optional<OidcStateStore.StateValue> result = store.consume("state-1");

        assertThat(result).isPresent();
        assertThat(result.get().nonce()).isEqualTo("nonce-1");
        assertThat(result.get().frontendOrigin()).isEqualTo("http://localhost:5173");
    }

    @Test
    void consumeTreatsNonJsonValueAsLegacyRawNonce() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete("auth:oidc:state:state-1"))
                .thenReturn("legacy-raw-nonce-value");

        Optional<OidcStateStore.StateValue> result = store.consume("state-1");

        assertThat(result).isPresent();
        assertThat(result.get().nonce()).isEqualTo("legacy-raw-nonce-value");
        assertThat(result.get().frontendOrigin()).isNull();
    }

    @Test
    void consumeReturnsEmptyWhenStateUnknown() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete(anyString())).thenReturn(null);

        assertThat(store.consume("unknown")).isEmpty();
    }
}
