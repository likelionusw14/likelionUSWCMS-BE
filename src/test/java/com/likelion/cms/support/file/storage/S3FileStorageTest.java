package com.likelion.cms.support.file.storage;

import com.likelion.cms.support.file.config.S3StorageProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3FileStorageTest {

    private static final String CHECKSUM_BASE64 =
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    @Mock
    private S3Client s3Client;

    private S3Presigner presigner;
    private S3FileStorage storage;

    @BeforeEach
    void setUp() {
        presigner = S3Presigner.builder()
                .region(Region.AP_NORTHEAST_2)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test-access-key", "test-secret-key")
                ))
                .build();
        storage = new S3FileStorage(
                s3Client,
                presigner,
                new S3StorageProperties(
                        "likelion-usw-cms-files",
                        "ap-northeast-2",
                        Duration.ofMinutes(10)
                )
        );
    }

    @AfterEach
    void tearDown() {
        presigner.close();
    }

    @Test
    void createsHttpsPresignedPutWithRequiredHeaders() {
        FileStorage.PresignedUpload upload = storage.createUploadUrl(
                "learning-resource/2026/07/25/id.pdf",
                "application/pdf",
                CHECKSUM_BASE64
        );

        assertThat(upload.uploadUrl()).startsWith("https://");
        assertThat(upload.requiredHeaders())
                .containsEntry("content-type", "application/pdf")
                .containsEntry("x-amz-checksum-sha256", CHECKSUM_BASE64);
        assertThat(upload.expiresAt()).isAfter(java.time.OffsetDateTime.now());
    }

    @Test
    void doesNotRequireAnImplicitChecksumWhenClientDidNotProvideOne() {
        FileStorage.PresignedUpload upload = storage.createUploadUrl(
                "learning-resource/2026/07/25/id.pdf",
                "application/pdf",
                null
        );

        assertThat(upload.requiredHeaders())
                .containsEntry("content-type", "application/pdf")
                .doesNotContainKeys("x-amz-checksum-crc32", "x-amz-checksum-sha256");
    }

    @Test
    void readsS3ObjectMetadataWithChecksum() {
        when(s3Client.headObject(any(software.amazon.awssdk.services.s3.model.HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder()
                        .contentType("application/pdf")
                        .contentLength(1024L)
                        .checksumSHA256(CHECKSUM_BASE64)
                        .build());

        Optional<FileStorage.StoredFileMetadata> metadata =
                storage.findMetadata("learning-resource/2026/07/25/id.pdf");

        assertThat(metadata).contains(new FileStorage.StoredFileMetadata(
                "application/pdf",
                1024L,
                CHECKSUM_BASE64
        ));
    }

    @Test
    void treatsForbiddenHeadAsMissingToAvoidObjectDisclosure() {
        when(s3Client.headObject(any(software.amazon.awssdk.services.s3.model.HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(403).message("forbidden").build());

        assertThat(storage.findMetadata("learning-resource/2026/07/25/missing.pdf"))
                .isEmpty();
    }
}
