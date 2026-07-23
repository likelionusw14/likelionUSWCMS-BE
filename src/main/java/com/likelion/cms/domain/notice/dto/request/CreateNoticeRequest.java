package com.likelion.cms.domain.notice.dto.request;

import com.likelion.cms.domain.notice.entity.NoticeTag;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.net.URI;

public record CreateNoticeRequest(
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 20000) String content,
        @NotNull NoticeTag tag,
        Boolean isFixed,
        @Size(max = 2048) String externalUrl,
        @Positive Long imageAssetId
) {
    @AssertTrue(message = "externalUrl은 http 또는 https URL이어야 합니다.")
    public boolean isExternalUrlValid() {
        return isHttpUrl(externalUrl);
    }

    static boolean isHttpUrl(String value) {
        if (value == null) {
            return true;
        }
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            return uri.getHost() != null
                    && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
