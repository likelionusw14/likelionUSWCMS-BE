package com.likelion.cms.domain.user.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.likelion.cms.common.type.PartType;
import com.likelion.cms.domain.cohort.dto.response.CohortSummary;
import com.likelion.cms.domain.user.entity.AccountStatus;
import com.likelion.cms.domain.user.entity.AppUser;
import com.likelion.cms.domain.user.entity.SystemRole;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

// 관리자 회원 API의 공통 응답 DTO. 필드명(role/status)과 구성은
// OpenAPI 스펙의 AccountResponse 스키마를 그대로 따름, 승인자/거절자/처리시각 같은 내부 감사용 필드는 응답에 노출하지 않음
// (엔티티엔 계속 남아있고, 나중에 별도 감사 로그 API가 필요하면 그때 노출).
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountResponse {

    private final Long userId;
    private final String name;
    private final String department;
    private final String studentId;
    private final CohortSummary cohort;
    private final PartType part;
    private final SystemRole role;
    private final AccountStatus status;
    private final String rejectionReason;

    /**
     * version : 낙관적 락(optimistic locking)을 위한 버전 값입니다.
     * 수정 요청 시 클라이언트가 조회 시점의 이 값을 그대로 전달해야 하며,
     * 서버에 저장된 현재 버전과 다르면 충돌로 간주해 요청이 거부됩니다.
     */
    private final Integer version;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static AccountResponse of(
            Long userId, String name, String department, String studentId, CohortSummary cohort,
            PartType part, SystemRole role, AccountStatus status, String rejectionReason,
            Integer version, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new AccountResponse(userId, name, department, studentId, cohort, part, role, status,
                rejectionReason, version, createdAt, updatedAt);
    }

    public static AccountResponse from(AppUser appUser) {
        return of(
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
