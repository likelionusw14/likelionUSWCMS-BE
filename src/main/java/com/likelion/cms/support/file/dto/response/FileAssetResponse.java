package com.likelion.cms.support.file.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import com.likelion.cms.common.type.FilePurpose;
import java.time.OffsetDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileAssetResponse {

    private final Long fileAssetId;
    private final FilePurpose purpose;
    private final String originalFileName;
    private final String mimeType;
    private final Long sizeBytes;
    private final OffsetDateTime createdAt;

    public static FileAssetResponse of(Long fileAssetId, FilePurpose purpose, String originalFileName,
                                       String mimeType, Long sizeBytes, OffsetDateTime createdAt) {
        return new FileAssetResponse(fileAssetId, purpose, originalFileName, mimeType, sizeBytes, createdAt);
    }
}