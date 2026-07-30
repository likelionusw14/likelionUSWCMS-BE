package com.likelion.cms.support.file.storage;

import com.likelion.cms.global.exception.BusinessException;
import com.likelion.cms.global.exception.ErrorCode;
import com.likelion.cms.support.file.config.S3StorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ChecksumMode;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import software.amazon.awssdk.core.sync.RequestBody;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3FileStorage implements FileStorage {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3StorageProperties properties;

    @Override
    public PresignedUpload createUploadUrl(String objectKey, String mimeType,
                                           String checksumSha256Base64) {
        assertConfigured();
        try {
            PutObjectRequest.Builder putObject = PutObjectRequest.builder()
                    .bucket(properties.bucketName())
                    .key(objectKey)
                    .contentType(mimeType);
            if (checksumSha256Base64 != null) {
                putObject.checksumSHA256(checksumSha256Base64);
            }

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(properties.uploadUrlTtl())
                    .putObjectRequest(putObject.build())
                    .build();
            PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);

            return new PresignedUpload(
                    presigned.url().toString(),
                    requiredHeaders(presigned.signedHeaders()),
                    OffsetDateTime.ofInstant(presigned.expiration(), ZoneOffset.UTC)
            );
        } catch (SdkException exception) {
            log.error("Failed to create an S3 presigned upload URL", exception);
            throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR);
        }
    }

    @Override
    public void uploadDirectly(String objectKey, byte[] content, String mimeType) {
        assertConfigured();
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(properties.bucketName())
                            .key(objectKey)
                            .contentType(mimeType)
                            .build(),
                    RequestBody.fromBytes(content)
            );
        } catch (SdkException exception) {
            log.error("Failed to upload file directly to S3", exception);
            throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR);
        }
    }

    @Override
    public PresignedDownload createDownloadUrl(String objectKey) {
        assertConfigured();
        try {
            GetObjectRequest getObject = GetObjectRequest.builder()
                    .bucket(properties.bucketName())
                    .key(objectKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(properties.uploadUrlTtl())
                    .getObjectRequest(getObject)
                    .build();
            PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);

            return new PresignedDownload(
                    presigned.url().toString(),
                    OffsetDateTime.ofInstant(presigned.expiration(), ZoneOffset.UTC)
            );
        } catch (SdkException exception) {
            log.error("Failed to create an S3 presigned download URL", exception);
            throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR);
        }
    }

    @Override
    public Optional<StoredFileMetadata> findMetadata(String objectKey) {
        assertConfigured();
        try {
            HeadObjectResponse response = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(properties.bucketName())
                    .key(objectKey)
                    .checksumMode(ChecksumMode.ENABLED)
                    .build());
            return Optional.of(new StoredFileMetadata(
                    response.contentType(),
                    response.contentLength(),
                    response.checksumSHA256()
            ));
        } catch (S3Exception exception) {
            if (exception.statusCode() == 403 || exception.statusCode() == 404) {
                return Optional.empty();
            }
            log.error("Failed to read S3 object metadata, status={}", exception.statusCode(), exception);
            throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR);
        } catch (SdkException exception) {
            log.error("Failed to read S3 object metadata", exception);
            throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR);
        }
    }

    private Map<String, String> requiredHeaders(Map<String, java.util.List<String>> signedHeaders) {
        Map<String, String> requiredHeaders = new LinkedHashMap<>();
        signedHeaders.forEach((name, values) -> {
            if (!"host".equalsIgnoreCase(name)) {
                requiredHeaders.put(name, String.join(",", values));
            }
        });
        return Map.copyOf(requiredHeaders);
    }

    private void assertConfigured() {
        if (!StringUtils.hasText(properties.bucketName())
                || !StringUtils.hasText(properties.region())
                || properties.uploadUrlTtl() == null
                || properties.uploadUrlTtl().isNegative()
                || properties.uploadUrlTtl().isZero()) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR);
        }
    }
}
