package com.likelion.cms.domain.user.dto.request;

import com.likelion.cms.domain.user.entity.SystemRole;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

// 권한 변경 요청. 이 요청 자체엔 "본인 강등 방지" 같은 비즈니스 규칙이 없음 -
// 그건 UserService.changeRole()에서 처리 (DTO는 형식 검증만 담당).
public record ChangeAccountRoleRequest(
        @NotNull SystemRole systemRole,
        @NotNull @PositiveOrZero Integer version
) {
}
