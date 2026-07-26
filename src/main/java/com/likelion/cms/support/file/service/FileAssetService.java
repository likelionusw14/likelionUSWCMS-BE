package com.likelion.cms.support.file.service;

import com.likelion.cms.global.exception.BusinessException;
import com.likelion.cms.global.exception.ErrorCode;
import com.likelion.cms.support.file.dto.request.FileAssetRequest;
import com.likelion.cms.support.file.dto.request.FileUploadUrlRequest;
import com.likelion.cms.support.file.dto.response.FileAssetResponse;
import com.likelion.cms.support.file.dto.response.FileUploadUrlResponse;
import com.likelion.cms.support.file.service.FileUploadPolicy.ValidatedFile;
import com.likelion.cms.support.file.storage.FileStorage;
import com.likelion.cms.support.file.storage.FileStorage.PresignedUpload;
import com.likelion.cms.support.file.storage.FileStorage.StoredFileMetadata;
import com.likelion.cms.support.file.store.FileIdempotencyRecord;
import com.likelion.cms.support.file.store.FileIdempotencyStore;
import com.likelion.cms.support.file.store.FileUploadGrant;
import com.likelion.cms.support.file.store.FileUploadGrantStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileAssetService {

    private final FileUploadPolicy fileUploadPolicy;
    private final FileObjectKeyFactory objectKeyFactory;
    private final FileStorage fileStorage;
    private final FileUploadGrantStore uploadGrantStore;
    private final FileIdempotencyStore idempotencyStore;
    private final FileAssetRegistrationService registrationService;

    public FileUploadUrlResponse createUploadUrl(FileUploadUrlRequest request, Long actorUserId) {
        ValidatedFile file = fileUploadPolicy.validate(
                request.purpose(),
                request.originalFileName(),
                request.mimeType(),
                request.sizeBytes(),
                request.checksumSha256()
        );
        String objectKey = objectKeyFactory.create(request.purpose(), file.canonicalExtension());
        PresignedUpload upload = fileStorage.createUploadUrl(
                objectKey,
                file.mimeType(),
                fileUploadPolicy.checksumBase64(file.checksumSha256())
        );

        uploadGrantStore.save(new FileUploadGrant(
                actorUserId,
                request.purpose(),
                objectKey,
                request.originalFileName(),
                file.mimeType(),
                file.sizeBytes(),
                file.checksumSha256()
        ));
        return FileUploadUrlResponse.of(
                upload.uploadUrl(),
                objectKey,
                upload.requiredHeaders(),
                upload.expiresAt()
        );
    }

    public FileAssetResponse completeUpload(FileAssetRequest request, Long actorUserId,
                                            UUID idempotencyKey) {
        ValidatedFile file = fileUploadPolicy.validate(
                request.purpose(),
                request.originalFileName(),
                request.mimeType(),
                request.sizeBytes(),
                request.checksumSha256()
        );

        FileIdempotencyRecord existing = idempotencyStore.find(actorUserId, idempotencyKey)
                .orElse(null);
        if (existing != null) {
            return handleExisting(existing, request.objectKey(), actorUserId);
        }
        if (!idempotencyStore.reserve(actorUserId, idempotencyKey, request.objectKey())) {
            FileIdempotencyRecord raced = idempotencyStore.find(actorUserId, idempotencyKey)
                    .orElseThrow(() -> new BusinessException(ErrorCode.FILE_IDEMPOTENCY_CONFLICT));
            return handleExisting(raced, request.objectKey(), actorUserId);
        }

        try {
            FileUploadGrant grant = uploadGrantStore.find(request.objectKey())
                    .orElseThrow(() -> new BusinessException(ErrorCode.FILE_UPLOAD_NOT_FOUND));
            verifyGrant(grant, request, actorUserId, file);

            StoredFileMetadata metadata = fileStorage.findMetadata(request.objectKey())
                    .orElseThrow(() -> new BusinessException(ErrorCode.FILE_UPLOAD_NOT_FOUND));
            verifyStoredMetadata(metadata, file);

            FileAssetResponse response = registrationService.register(grant);
            idempotencyStore.complete(
                    actorUserId,
                    idempotencyKey,
                    request.objectKey(),
                    response.getFileAssetId()
            );
            uploadGrantStore.delete(request.objectKey());
            return response;
        } catch (RuntimeException exception) {
            idempotencyStore.clearPending(actorUserId, idempotencyKey, request.objectKey());
            throw exception;
        }
    }

    private FileAssetResponse handleExisting(FileIdempotencyRecord record, String objectKey,
                                             Long actorUserId) {
        if (!Objects.equals(record.objectKey(), objectKey)
                || record.status() == FileIdempotencyRecord.Status.PENDING
                || record.fileAssetId() == null) {
            throw new BusinessException(ErrorCode.FILE_IDEMPOTENCY_CONFLICT);
        }
        return registrationService.findOwned(record.fileAssetId(), actorUserId, objectKey);
    }

    private void verifyGrant(FileUploadGrant grant, FileAssetRequest request,
                             Long actorUserId, ValidatedFile file) {
        if (!grant.matches(
                actorUserId,
                request.purpose(),
                request.objectKey(),
                request.originalFileName(),
                file.mimeType(),
                file.sizeBytes(),
                file.checksumSha256()
        )) {
            throw new BusinessException(ErrorCode.FILE_METADATA_MISMATCH);
        }
    }

    private void verifyStoredMetadata(StoredFileMetadata metadata, ValidatedFile expected) {
        boolean checksumMatches = expected.checksumSha256() == null
                || Objects.equals(
                fileUploadPolicy.checksumBase64(expected.checksumSha256()),
                metadata.checksumSha256Base64()
        );
        if (!Objects.equals(expected.mimeType(), metadata.mimeType())
                || expected.sizeBytes() != metadata.sizeBytes()
                || !checksumMatches) {
            throw new BusinessException(ErrorCode.FILE_METADATA_MISMATCH);
        }
    }
}
