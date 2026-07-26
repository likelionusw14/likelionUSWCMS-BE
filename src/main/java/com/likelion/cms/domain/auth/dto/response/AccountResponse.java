package com.likelion.cms.domain.auth.dto.response;

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
    private final int version;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static AccountResponse of(Long userId, String name, String department, String studentId,
                                      CohortSummary cohort, PartType part, SystemRole role, AccountStatus status,
                                      String rejectionReason, int version,
                                      LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new AccountResponse(userId, name, department, studentId, cohort, part, role, status,
                rejectionReason, version, createdAt, updatedAt);
    }

    public static AccountResponse from(AppUser user) {
        CohortSummary cohort = CohortSummary.of(
                user.getCohort().getCohortId(), user.getCohort().getNumber(), user.getCohort().getName());
        return of(user.getUserId(), user.getName(), user.getDepartment(), user.getStudentId(),
                cohort, user.getPart(), user.getSystemRole(), user.getAccountStatus(),
                user.getRejectionReason(), user.getVersion(), user.getCreatedAt(), user.getUpdatedAt());
    }
}
