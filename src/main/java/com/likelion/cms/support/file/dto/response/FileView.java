package com.likelion.cms.support.file.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)

public class FileView {
    private final FileAssetResponse file;
    private final String downloadUrl;
    private final OffsetDateTime expiresAt;

    public static FileView of(FileAssetResponse file, String downloadUrl, OffsetDateTime expiresAt) {
        return new FileView(file, downloadUrl, expiresAt);
    }

}
