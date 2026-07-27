package com.likelion.cms.domain.certificate.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.likelion.cms.common.type.PartType;
import com.likelion.cms.domain.cohort.dto.response.CohortSummary;
import com.likelion.cms.domain.user.entity.AppUser;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CertificatePreviewResponse {

    private final String name;
    private final String department;
    private final String studentId;
    private final CohortSummary cohort;
    private final PartType part;
    private final LocalDate activityStartedAt;
    private final LocalDate activityEndedAt;

    public static CertificatePreviewResponse of(String name, String department, String studentId,
                                                CohortSummary cohort, PartType part,
                                                LocalDate activityStartedAt, LocalDate activityEndedAt) {
        return new CertificatePreviewResponse(name, department, studentId, cohort, part,
                activityStartedAt, activityEndedAt);
    }

    public static CertificatePreviewResponse from(AppUser user) {
        CohortSummary cohortSummary = CohortSummary.of(
                user.getCohort().getCohortId(),
                user.getCohort().getNumber(),
                user.getCohort().getName()
        );

        return of(
                user.getName(),
                user.getDepartment(),
                user.getStudentId(),
                cohortSummary,
                user.getPart(),
                null,  // activityStartedAt - Cohort에 getStartedAt() 있는지 확인 필요
                null   // activityEndedAt - Cohort에 getEndedAt() 있는지 확인 필요
        );
    }
}