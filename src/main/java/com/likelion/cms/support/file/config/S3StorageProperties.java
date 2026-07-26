package com.likelion.cms.support.file.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "storage.s3")
public record S3StorageProperties(
        @NotBlank String bucketName,
        String region,
        Duration uploadUrlTtl
) {
}
