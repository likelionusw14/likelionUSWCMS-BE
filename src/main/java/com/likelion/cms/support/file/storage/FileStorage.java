package com.likelion.cms.support.file.storage;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

public interface FileStorage {

    PresignedUpload createUploadUrl(String objectKey, String mimeType, String checksumSha256Base64);

    PresignedDownload createDownloadUrl(String objectKey);

    void uploadDirectly(String objectKey, byte[] content, String mimeType);

    Optional<StoredFileMetadata> findMetadata(String objectKey);

    record PresignedUpload(
            String uploadUrl,
            Map<String, String> requiredHeaders,
            OffsetDateTime expiresAt
    ) {
    }

    record PresignedDownload(
            String downloadUrl,
            OffsetDateTime expiresAt
    ) {
    }

    record StoredFileMetadata(
            String mimeType,
            long sizeBytes,
            String checksumSha256Base64
    ) {
    }
}