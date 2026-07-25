package com.likelion.cms.support.file.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.likelion.cms.support.file.entity.FileAsset;
import com.likelion.cms.support.file.entity.FilePurpose;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileAssetResponse {

    private final Long fileAssetId;
    private final FilePurpose purpose;
    private final String originalFileName;
    private final String mimeType;
    private final Long sizeBytes;
    private final LocalDateTime createdAt;

    public static FileAssetResponse of(Long fileAssetId, FilePurpose purpose, String originalFileName,
                                       String mimeType, Long sizeBytes, LocalDateTime createdAt) {
        return new FileAssetResponse(fileAssetId, purpose, originalFileName, mimeType, sizeBytes, createdAt);
    }

    public static FileAssetResponse from(FileAsset fileAsset) {
        return of(fileAsset.getFileAssetId(), fileAsset.getPurpose(), fileAsset.getOriginalFileName(),
                fileAsset.getMimeType(), fileAsset.getSizeBytes(), fileAsset.getCreatedAt());
    }
}
