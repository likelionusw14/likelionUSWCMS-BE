package com.likelion.cms.support.file.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "storage.s3")
public record S3StorageProperties(
        String bucketName,
        String region,
        Duration uploadUrlTtl
) {
}
