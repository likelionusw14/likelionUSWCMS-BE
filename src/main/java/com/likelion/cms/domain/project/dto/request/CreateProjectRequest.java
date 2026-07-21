package com.likelion.cms.domain.project.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.net.URI;
import java.time.LocalDate;

public record CreateProjectRequest(
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 20000) String description,
        @NotBlank @Size(max = 50) String projectType,
        @Positive Long thumbnailAssetId,
        @Size(max = 2048) String deployUrl,
        @Size(max = 2048) String githubUrl,
        @NotNull @Positive Long cohortId,
        @NotNull LocalDate startedMonth,
        @NotNull LocalDate endedMonth
) {
    @AssertTrue(message = "deployUrl은 http 또는 https URL이어야 합니다.")
    public boolean isDeployUrlValid() {
        return isHttpUrl(deployUrl);
    }

    @AssertTrue(message = "githubUrl은 http 또는 https URL이어야 합니다.")
    public boolean isGithubUrlValid() {
        return isHttpUrl(githubUrl);
    }

    @AssertTrue(message = "endedMonth는 startedMonth보다 빠를 수 없습니다.")
    public boolean isDateRangeValid() {
        return startedMonth == null || endedMonth == null || !endedMonth.isBefore(startedMonth);
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
