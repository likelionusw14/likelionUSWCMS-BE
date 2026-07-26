package com.likelion.cms.support.file.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.Map;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileUploadUrlResponse {

    private final String uploadUrl;
    private final String objectKey;
    private final Map<String, String> requiredHeaders;
    private final OffsetDateTime expiresAt;

    public static FileUploadUrlResponse of(String uploadUrl, String objectKey,
                                           Map<String, String> requiredHeaders,
                                           OffsetDateTime expiresAt) {
        return new FileUploadUrlResponse(uploadUrl, objectKey, requiredHeaders, expiresAt);
    }
}
