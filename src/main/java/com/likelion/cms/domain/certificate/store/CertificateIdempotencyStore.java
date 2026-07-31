package com.likelion.cms.domain.certificate.store;

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
public class CertificateIdempotencyStore {

    static final Duration PENDING_TTL = Duration.ofMinutes(10);
    static final Duration COMPLETED_TTL = Duration.ofHours(24);
    private static final String KEY_PREFIX = "certificate:idempotency:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<CertificateIdempotencyRecord> find(Long actorUserId, UUID idempotencyKey) {
        String value = redisTemplate.opsForValue().get(key(actorUserId, idempotencyKey));
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(deserialize(value));
    }

    public boolean reserve(Long actorUserId, UUID idempotencyKey) {
        Boolean reserved = redisTemplate.opsForValue().setIfAbsent(
                key(actorUserId, idempotencyKey),
                serialize(CertificateIdempotencyRecord.pending()),
                PENDING_TTL
        );
        return Boolean.TRUE.equals(reserved);
    }

    public void complete(Long actorUserId, UUID idempotencyKey, Long certificateId) {
        redisTemplate.opsForValue().set(
                key(actorUserId, idempotencyKey),
                serialize(CertificateIdempotencyRecord.completed(certificateId)),
                COMPLETED_TTL
        );
    }

    public void clearPending(Long actorUserId, UUID idempotencyKey) {
        Optional<CertificateIdempotencyRecord> current = find(actorUserId, idempotencyKey);
        if (current.filter(record -> record.status() == CertificateIdempotencyRecord.Status.PENDING)
                .isPresent()) {
            redisTemplate.delete(key(actorUserId, idempotencyKey));
        }
    }

    private String key(Long actorUserId, UUID idempotencyKey) {
        return KEY_PREFIX + actorUserId + ":" + idempotencyKey;
    }

    private String serialize(CertificateIdempotencyRecord record) {
        try {
            return objectMapper.writeValueAsString(record);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("활동증명서 멱등 기록을 직렬화할 수 없습니다.", exception);
        }
    }

    private CertificateIdempotencyRecord deserialize(String value) {
        try {
            return objectMapper.readValue(value, CertificateIdempotencyRecord.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("활동증명서 멱등 기록을 역직렬화할 수 없습니다.", exception);
        }
    }
}