package com.likelion.cms.domain.attendance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AttendanceCheckInRequest(
        @NotBlank @Pattern(regexp = "^\\d{6}$") String code
) {
}
