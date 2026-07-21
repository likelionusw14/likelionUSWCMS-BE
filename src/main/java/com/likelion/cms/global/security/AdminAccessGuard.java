package com.likelion.cms.global.security;

import com.likelion.cms.domain.user.entity.SystemRole;
import com.likelion.cms.global.exception.BusinessException;
import com.likelion.cms.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

// 관리자 전용 API 진입점에서 공통으로 쓰는 인가 체크.
// 이 클래스는 이 브랜치에서 새로 만든 게 아니라, 팀원의 다른 PR(feat/2, 이슈#2)에서
// 먼저 정의한 걸 그대로 가져와서 씀 - 파일 경로/내용 동일 (나중에 feat/2가
// develop에 먼저 머지되면, 이 사본은 지우고 리베이스 예정).
@Component
public class AdminAccessGuard {

    public Long requireAdmin(CurrentUserPrincipal principal) {
        // 비로그인 상태 (principal 자체가 없음)
        if (principal == null || principal.userId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        // 로그인은 했지만 관리자가 아님
        if (principal.role() != SystemRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return principal.userId();
    }
}
