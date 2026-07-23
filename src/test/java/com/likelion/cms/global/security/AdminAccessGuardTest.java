package com.likelion.cms.global.security;

import com.likelion.cms.domain.user.entity.SystemRole;
import com.likelion.cms.global.exception.BusinessException;
import com.likelion.cms.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminAccessGuardTest {

    private final AdminAccessGuard adminAccessGuard = new AdminAccessGuard();

    @Test
    void returnsUserIdForAdmin() {
        assertThat(adminAccessGuard.requireAdmin(new CurrentUserPrincipal(1L, SystemRole.ADMIN)))
                .isEqualTo(1L);
    }

    @Test
    void rejectsUnauthenticatedRequest() {
        assertThatThrownBy(() -> adminAccessGuard.requireAdmin(null))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    void rejectsMemberRole() {
        CurrentUserPrincipal principal = new CurrentUserPrincipal(1L, SystemRole.MEMBER);

        assertThatThrownBy(() -> adminAccessGuard.requireAdmin(principal))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }
}
