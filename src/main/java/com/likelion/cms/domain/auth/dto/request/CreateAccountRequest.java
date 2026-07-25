package com.likelion.cms.domain.auth.dto.request;

import com.likelion.cms.common.type.PartType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Size(max = 100) String department,
        @NotBlank @Size(max = 30) @Pattern(regexp = "^[A-Za-z0-9-]+$") String studentId,
        @NotNull @Positive Long cohortId,
        @NotNull PartType part
) {
}
