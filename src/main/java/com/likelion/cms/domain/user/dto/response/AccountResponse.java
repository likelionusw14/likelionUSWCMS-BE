package com.likelion.cms.domain.user.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.likelion.cms.common.type.PartType;
import com.likelion.cms.domain.cohort.dto.response.CohortSummary;
import com.likelion.cms.domain.user.entity.AccountStatus;
import com.likelion.cms.domain.user.entity.AppUser;
import com.likelion.cms.domain.user.entity.SystemRole;

import java.time.LocalDateTime;

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
