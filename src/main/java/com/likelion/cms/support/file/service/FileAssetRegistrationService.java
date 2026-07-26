package com.likelion.cms.support.file.service;

import com.likelion.cms.domain.user.entity.AppUser;
import com.likelion.cms.domain.user.repository.AppUserRepository;
import com.likelion.cms.global.exception.BusinessException;
import com.likelion.cms.global.exception.ErrorCode;
import com.likelion.cms.support.file.dto.response.FileAssetResponse;
import com.likelion.cms.support.file.entity.FileAsset;
import com.likelion.cms.support.file.repository.FileAssetRepository;
import com.likelion.cms.support.file.store.FileUploadGrant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FileAssetRegistrationService {

    private final FileAssetRepository fileAssetRepository;
    private final AppUserRepository appUserRepository;

    @Transactional
    public FileAssetResponse register(FileUploadGrant grant) {
        return fileAssetRepository.findByObjectKey(grant.objectKey())
                .map(existing -> existingResponse(existing, grant))
                .orElseGet(() -> create(grant));
    }

    @Transactional(readOnly = true)
    public FileAssetResponse findOwned(Long fileAssetId, Long actorUserId, String objectKey) {
        FileAsset fileAsset = fileAssetRepository.findById(fileAssetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_UPLOAD_NOT_FOUND));
        if (!Objects.equals(fileAsset.getUploadedByUser().getUserId(), actorUserId)
                || !Objects.equals(fileAsset.getObjectKey(), objectKey)) {
            throw new BusinessException(ErrorCode.FILE_IDEMPOTENCY_CONFLICT);
        }
        return FileAssetResponse.from(fileAsset);
    }

    private FileAssetResponse create(FileUploadGrant grant) {
        AppUser actor = appUserRepository.findById(grant.actorUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        FileAsset fileAsset = FileAsset.builder()
                .purpose(grant.purpose())
                .objectKey(grant.objectKey())
                .originalFileName(grant.originalFileName())
                .mimeType(grant.mimeType())
                .sizeBytes(grant.sizeBytes())
                .checksumSha256(grant.checksumSha256())
                .uploadedByUser(actor)
                .build();
        return FileAssetResponse.from(fileAssetRepository.saveAndFlush(fileAsset));
    }

    private FileAssetResponse existingResponse(FileAsset existing, FileUploadGrant grant) {
        if (!Objects.equals(existing.getUploadedByUser().getUserId(), grant.actorUserId())
                || existing.getPurpose() != grant.purpose()
                || !Objects.equals(existing.getOriginalFileName(), grant.originalFileName())
                || !Objects.equals(existing.getMimeType(), grant.mimeType())
                || !Objects.equals(existing.getSizeBytes(), grant.sizeBytes())
                || !Objects.equals(existing.getChecksumSha256(), grant.checksumSha256())) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        return FileAssetResponse.from(existing);
    }
}
