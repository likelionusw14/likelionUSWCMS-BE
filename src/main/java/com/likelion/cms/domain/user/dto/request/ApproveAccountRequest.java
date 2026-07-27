package com.likelion.cms.domain.user.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

// 가입 승인 요청. 사유는 필요 없지만, 다른 PATCH들과 동일하게
// 낙관적 락(version)은 항상 요구함 - 승인 직전에 다른 관리자가 이미
// 처리했을 수 있는 경합 상황을 놓치지 않기 위함.
public record ApproveAccountRequest(
        @NotNull @PositiveOrZero Integer version
) {
}
