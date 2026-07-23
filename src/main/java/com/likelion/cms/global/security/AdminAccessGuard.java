package com.likelion.cms.global.security;

import com.likelion.cms.domain.user.entity.SystemRole;
import com.likelion.cms.global.exception.BusinessException;
import com.likelion.cms.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class AdminAccessGuard {

    public Long requireAdmin(CurrentUserPrincipal principal) {
        if (principal == null || principal.userId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (principal.role() != SystemRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return principal.userId();
    }
}
