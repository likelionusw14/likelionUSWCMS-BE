package com.likelion.cms.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

// 가입 거절 요청. 승인과 달리 사유가 필수라 별도 필드가 있음.
public record RejectAccountRequest(
        @NotBlank @Size(max = 500) String rejectionReason,
        @NotNull @PositiveOrZero Integer version
) {
}
