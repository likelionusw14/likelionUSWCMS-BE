package com.likelion.cms.support.file.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FileUploadGrantStore {

    static final Duration TTL = Duration.ofMinutes(10);
    private static final String KEY_PREFIX = "file:upload-grant:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void save(FileUploadGrant grant) {
        redisTemplate.opsForValue().set(key(grant.objectKey()), serialize(grant), TTL);
    }

    public Optional<FileUploadGrant> find(String objectKey) {
        String value = redisTemplate.opsForValue().get(key(objectKey));
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(deserialize(value));
    }

    public void delete(String objectKey) {
        redisTemplate.delete(key(objectKey));
    }

    private String key(String objectKey) {
        return KEY_PREFIX + sha256Hex(objectKey);
    }

    private String serialize(FileUploadGrant grant) {
        try {
            return objectMapper.writeValueAsString(grant);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("파일 업로드 발급 기록을 직렬화할 수 없습니다.", exception);
        }
    }

    private FileUploadGrant deserialize(String value) {
        try {
            return objectMapper.readValue(value, FileUploadGrant.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("파일 업로드 발급 기록을 역직렬화할 수 없습니다.", exception);
        }
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm not available", exception);
        }
    }
}
