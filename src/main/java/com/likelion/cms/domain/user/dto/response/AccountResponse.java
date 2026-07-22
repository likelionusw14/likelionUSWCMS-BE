package com.likelion.cms.domain.user.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.likelion.cms.common.type.PartType;
import com.likelion.cms.domain.cohort.dto.response.CohortSummary;
import com.likelion.cms.domain.user.entity.AccountStatus;
import com.likelion.cms.domain.user.entity.AppUser;
import com.likelion.cms.domain.user.entity.SystemRole;

import java.time.LocalDateTime;

// 관리자 회원 API의 공통 응답 DTO. 필드명(role/status)과 구성은
// OpenAPI 스펙의 AccountResponse 스키마를 그대로 따름 - 프론트가 이 스펙 기준으로
// 개발하기 때문에, 승인자/거절자/처리시각 같은 내부 감사용 필드는 응답에 노출하지 않음
// (엔티티엔 계속 남아있고, 나중에 별도 감사 로그 API가 필요하면 그때 노출).
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AccountResponse(
        Long userId,
        String name,
        String department,
        String studentId,
        CohortSummary cohort,
        PartType part,
        SystemRole role,
        AccountStatus status,
        String rejectionReason,
        Integer version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AccountResponse from(AppUser appUser) {
        return new AccountResponse(
                appUser.getUserId(),
                appUser.getName(),
                appUser.getDepartment(),
                appUser.getStudentId(),
                CohortSummary.from(appUser.getCohort()),
                appUser.getPart(),
                appUser.getSystemRole(),
                appUser.getAccountStatus(),
                appUser.getRejectionReason(),
                appUser.getVersion(),
                appUser.getCreatedAt(),
                appUser.getUpdatedAt()
        );
    }
}
