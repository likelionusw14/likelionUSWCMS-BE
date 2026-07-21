package com.likelion.cms.domain.user.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.likelion.cms.common.type.PartType;
import com.likelion.cms.domain.cohort.dto.response.CohortSummary;
import com.likelion.cms.domain.user.entity.AccountStatus;
import com.likelion.cms.domain.user.entity.AppUser;
import com.likelion.cms.domain.user.entity.SystemRole;

import java.time.LocalDateTime;

// 관리자 회원 API의 공통 응답 DTO. AppUser 엔티티를 그대로 노출하지 않고
// 응답에 필요한 필드만 골라서 변환함 (kakaoSubject 같은 내부용 필드는 제외).
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AccountResponse(
        Long userId,
        String name,
        String department,
        String studentId,
        CohortSummary cohort,
        PartType part,
        SystemRole systemRole,
        AccountStatus accountStatus,
        LocalDateTime approvedAt,
        Long approvedBy,
        LocalDateTime rejectedAt,
        Long rejectedBy,
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
                appUser.getApprovedAt(),
                // 승인/거절 이력이 없으면 approvedByUser 자체가 null이라
                // .getUserId()를 바로 호출하면 NPE - 널 체크 후 ID만 뽑음.
                appUser.getApprovedByUser() == null ? null : appUser.getApprovedByUser().getUserId(),
                appUser.getRejectedAt(),
                appUser.getRejectedByUser() == null ? null : appUser.getRejectedByUser().getUserId(),
                appUser.getRejectionReason(),
                appUser.getVersion(),
                appUser.getCreatedAt(),
                appUser.getUpdatedAt()
        );
    }
}
