package com.likelion.cms.domain.resource.dto.request;

import com.likelion.cms.common.type.PartType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateLearningResourceRequest(
        @NotBlank @Size(max = 150) String title,
        @NotNull @Min(1) @Max(52) Integer week,
        @NotNull PartType targetPart,
        @NotNull @Positive Long fileAssetId
) {
}
