package com.likelion.cms.domain.user.dto.request;

import com.likelion.cms.domain.user.entity.SystemRole;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

// 권한 변경 요청 (스펙의 UpdateRoleRequest). "ACTIVE 회원만 변경 가능",
// "마지막 ADMIN의 MEMBER 변경 차단" 같은 비즈니스 규칙은 UserService에서 처리.
public record UpdateRoleRequest(
        @NotNull SystemRole role,
        @NotNull @PositiveOrZero Integer version
) {
}
