package com.likelion.cms.support.file.config;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class S3StorageConfigTest {

    @Test
    void buildsS3ClientAndPresignerForConfiguredRegion() {
        S3StorageProperties properties = new S3StorageProperties(
                "likelion-usw-cms-files",
                "ap-northeast-2",
                Duration.ofMinutes(10)
        );
        S3StorageConfig config = new S3StorageConfig();

        try (S3Client client = config.s3Client(properties);
             S3Presigner presigner = config.s3Presigner(properties)) {
            assertThat(client.serviceName()).isEqualTo("s3");
            assertThat(presigner).isNotNull();
        }
    }
}
