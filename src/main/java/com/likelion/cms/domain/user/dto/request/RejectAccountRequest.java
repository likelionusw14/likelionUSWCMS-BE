package com.likelion.cms.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectAccountRequest(
        @NotBlank @Size(max = 500) String rejectionReason
) {
}
