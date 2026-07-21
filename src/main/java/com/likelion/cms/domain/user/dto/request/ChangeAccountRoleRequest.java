package com.likelion.cms.domain.user.dto.request;

import com.likelion.cms.domain.user.entity.SystemRole;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ChangeAccountRoleRequest(
        @NotNull SystemRole systemRole,
        @NotNull @PositiveOrZero Integer version
) {
}
