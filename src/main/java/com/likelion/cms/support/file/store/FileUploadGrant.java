package com.likelion.cms.support.file.store;

import com.likelion.cms.support.file.entity.FilePurpose;

import java.util.Objects;

public record FileUploadGrant(
        Long actorUserId,
        FilePurpose purpose,
        String objectKey,
        String originalFileName,
        String mimeType,
        long sizeBytes,
        String checksumSha256
) {

    public boolean matches(Long requestedActorUserId, FilePurpose requestedPurpose,
                           String requestedObjectKey, String requestedOriginalFileName,
                           String requestedMimeType, long requestedSizeBytes,
                           String requestedChecksumSha256) {
        return Objects.equals(actorUserId, requestedActorUserId)
                && purpose == requestedPurpose
                && Objects.equals(objectKey, requestedObjectKey)
                && Objects.equals(originalFileName, requestedOriginalFileName)
                && Objects.equals(mimeType, requestedMimeType)
                && sizeBytes == requestedSizeBytes
                && Objects.equals(checksumSha256, requestedChecksumSha256);
    }
}
