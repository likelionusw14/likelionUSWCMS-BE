package com.likelion.cms.domain.user.dto.request;

import com.likelion.cms.domain.user.entity.AccountStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

// 가입 승인/거절을 하나로 합친 요청 (스펙의 UpdateAccountStatusRequest).
// status=ACTIVE(승인)일 때는 rejectionReason이 없어야 하고,
// status=REJECTED(거절)일 때는 rejectionReason이 필수 - 이 관계를 @AssertTrue로 검증.
public record UpdateAccountStatusRequest(
        @NotNull AccountStatus status,
        String rejectionReason,
        @NotNull @PositiveOrZero Integer version
) {
    @AssertTrue(message = "status는 ACTIVE 또는 REJECTED만 가능합니다.")
    public boolean isStatusValid() {
        return status == AccountStatus.ACTIVE || status == AccountStatus.REJECTED;
    }

    @AssertTrue(message = "REJECTED일 때는 rejectionReason이 필수이고, ACTIVE일 때는 비어 있어야 합니다.")
    public boolean isRejectionReasonValid() {
        if (status == AccountStatus.REJECTED) {
            return rejectionReason != null && !rejectionReason.isBlank() && rejectionReason.length() <= 500;
        }
        return rejectionReason == null;
    }
}
