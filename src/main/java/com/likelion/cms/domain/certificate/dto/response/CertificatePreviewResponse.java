package com.likelion.cms.domain.certificate.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.likelion.cms.common.type.PartType;
import com.likelion.cms.domain.certificate.dto.response.CohortSummaryDto;
import com.likelion.cms.domain.user.entity.AppUser;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CertificatePreviewResponse {

    private String name;
    private String department;
    private String studentId;
    private CohortSummaryDto cohort;
    private PartType part;
    private LocalDate activityStartedAt;
    private LocalDate activityEndedAt;

    public static CertificatePreviewResponse from(AppUser user) {
        return CertificatePreviewResponse.builder()
                .name(user.getName())
                .department(user.getDepartment())
                .studentId(user.getStudentId())
                .cohort(CohortSummaryDto.from(user.getCohort()))
                .part(user.getPart())
                .activityStartedAt(null)  // 아래 참고
                .activityEndedAt(null)    // 아래 참고
                .build();
    }
}