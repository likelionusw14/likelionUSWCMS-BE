package com.likelion.cms.support.file.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FileIdempotencyStore {

    static final Duration PENDING_TTL = Duration.ofMinutes(10);
    static final Duration COMPLETED_TTL = Duration.ofHours(24);
    private static final String KEY_PREFIX = "file:idempotency:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<FileIdempotencyRecord> find(Long actorUserId, UUID idempotencyKey) {
        String value = redisTemplate.opsForValue().get(key(actorUserId, idempotencyKey));
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(deserialize(value));
    }

    public boolean reserve(Long actorUserId, UUID idempotencyKey, String objectKey) {
        Boolean reserved = redisTemplate.opsForValue().setIfAbsent(
                key(actorUserId, idempotencyKey),
                serialize(FileIdempotencyRecord.pending(objectKey)),
                PENDING_TTL
        );
        return Boolean.TRUE.equals(reserved);
    }

    public void complete(Long actorUserId, UUID idempotencyKey,
                         String objectKey, Long fileAssetId) {
        redisTemplate.opsForValue().set(
                key(actorUserId, idempotencyKey),
                serialize(FileIdempotencyRecord.completed(objectKey, fileAssetId)),
                COMPLETED_TTL
        );
    }

    public void clearPending(Long actorUserId, UUID idempotencyKey, String objectKey) {
        Optional<FileIdempotencyRecord> current = find(actorUserId, idempotencyKey);
        if (current.filter(record -> record.status() == FileIdempotencyRecord.Status.PENDING)
                .filter(record -> record.objectKey().equals(objectKey))
                .isPresent()) {
            redisTemplate.delete(key(actorUserId, idempotencyKey));
        }
    }

    private String key(Long actorUserId, UUID idempotencyKey) {
        return KEY_PREFIX + actorUserId + ":" + idempotencyKey;
    }

    private String serialize(FileIdempotencyRecord record) {
        try {
            return objectMapper.writeValueAsString(record);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("파일 멱등 기록을 직렬화할 수 없습니다.", exception);
        }
    }

    private FileIdempotencyRecord deserialize(String value) {
        try {
            return objectMapper.readValue(value, FileIdempotencyRecord.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("파일 멱등 기록을 역직렬화할 수 없습니다.", exception);
        }
    }
}
