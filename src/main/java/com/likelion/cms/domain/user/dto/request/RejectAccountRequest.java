package com.likelion.cms.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 가입 거절 요청. 승인(approve)과 달리 거절은 사유가 필수라 별도 DTO로 분리.
public record RejectAccountRequest(
        @NotBlank @Size(max = 500) String rejectionReason
) {
}
