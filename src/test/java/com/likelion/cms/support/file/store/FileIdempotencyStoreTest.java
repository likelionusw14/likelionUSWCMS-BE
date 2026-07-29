package com.likelion.cms.support.file.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileIdempotencyStoreTest {

    private static final UUID KEY =
            UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private FileIdempotencyStore store;

    @BeforeEach
    void setUp() {
        store = new FileIdempotencyStore(redisTemplate, new ObjectMapper());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void reservesPendingRequestAtomicallyForTenMinutes() {
        when(valueOperations.setIfAbsent(
                anyString(),
                anyString(),
                eq(Duration.ofMinutes(10))
        )).thenReturn(true);

        assertThat(store.reserve(7L, KEY, "object.pdf")).isTrue();
        verify(valueOperations).setIfAbsent(
                eq("file:idempotency:7:" + KEY),
                anyString(),
                eq(FileIdempotencyStore.PENDING_TTL)
        );
    }

    @Test
    void completesRequestForTwentyFourHours() {
        store.complete(7L, KEY, "object.pdf", 10L);

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(
                eq("file:idempotency:7:" + KEY),
                valueCaptor.capture(),
                eq(FileIdempotencyStore.COMPLETED_TTL)
        );
        assertThat(valueCaptor.getValue())
                .contains("\"status\":\"COMPLETED\"")
                .contains("\"fileAssetId\":10");
    }

    @Test
    void readsCompletedRecord() {
        when(valueOperations.get(anyString())).thenReturn("""
                {"status":"COMPLETED","objectKey":"object.pdf","fileAssetId":10}
                """);

        assertThat(store.find(7L, KEY))
                .contains(FileIdempotencyRecord.completed("object.pdf", 10L));
    }
}
