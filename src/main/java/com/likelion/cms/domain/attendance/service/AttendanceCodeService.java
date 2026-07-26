package com.likelion.cms.domain.attendance.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AttendanceCodeService {

    private static final Duration CODE_TTL = Duration.ofSeconds(300);
    private static final String KEY_PREFIX = "attendance:code:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public AttendanceCodeCacheValue issue(Long scheduleId) {
        String code = generateSixDigitCode();
        AttendanceCodeCacheValue value = new AttendanceCodeCacheValue(code, LocalDateTime.now());

        redisTemplate.opsForValue().set(key(scheduleId), serialize(value), CODE_TTL);
        return value;
    }

    public Optional<AttendanceCodeCacheValue> getCurrent(Long scheduleId) {
        String json = redisTemplate.opsForValue().get(key(scheduleId));
        if (json == null) {
            return Optional.empty();
        }
        return Optional.of(deserialize(json));
    }

    public boolean matches(Long scheduleId, String inputCode) {
        return getCurrent(scheduleId)
                .map(v -> v.code().equals(inputCode))
                .orElse(false);
    }

    private String generateSixDigitCode() {
        int number = secureRandom.nextInt(1_000_000);
        return String.format("%06d", number);
    }

    private String key(Long scheduleId) {
        return KEY_PREFIX + scheduleId;
    }

    private String serialize(AttendanceCodeCacheValue value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("출결 코드 직렬화에 실패했습니다.", e);
        }
    }

    private AttendanceCodeCacheValue deserialize(String json) {
        try {
            return objectMapper.readValue(json, AttendanceCodeCacheValue.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("출결 코드 역직렬화에 실패했습니다.", e);
        }
    }
}