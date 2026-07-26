package com.likelion.cms.global.security;

import com.likelion.cms.domain.user.entity.AccountStatus;
import com.likelion.cms.domain.user.repository.AppUserRepository;
import com.likelion.cms.global.exception.BusinessException;
import com.likelion.cms.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountStatusGuard {

    private final AppUserRepository appUserRepository;

    public Long requireActive(CurrentUserPrincipal principal) {
        if (principal == null || principal.userId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        boolean active = appUserRepository.findById(principal.userId())
                .map(user -> user.getAccountStatus() == AccountStatus.ACTIVE)
                .orElse(false);
        if (!active) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return principal.userId();
    }
}
