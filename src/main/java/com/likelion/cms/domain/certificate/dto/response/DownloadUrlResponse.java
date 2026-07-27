package com.likelion.cms.domain.certificate.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DownloadUrlResponse {

    private final String downloadUrl;
    private final OffsetDateTime expiresAt;

    public static DownloadUrlResponse of(String downloadUrl, OffsetDateTime expiresAt) {
        return new DownloadUrlResponse(downloadUrl, expiresAt);
    }
}