package com.likelion.cms.support.file.dto.response;

import com.likelion.cms.support.file.entity.FileAsset;
import com.likelion.cms.support.file.entity.FilePurpose;

import java.time.LocalDateTime;

public record FileAssetResponse(
        Long fileAssetId,
        FilePurpose purpose,
        String originalFileName,
        String mimeType,
        Long sizeBytes,
        LocalDateTime createdAt
) {
    public static FileAssetResponse from(FileAsset fileAsset) {
        return new FileAssetResponse(
                fileAsset.getFileAssetId(),
                fileAsset.getPurpose(),
                fileAsset.getOriginalFileName(),
                fileAsset.getMimeType(),
                fileAsset.getSizeBytes(),
                fileAsset.getCreatedAt()
        );
    }
}
