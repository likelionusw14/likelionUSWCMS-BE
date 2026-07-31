package com.likelion.cms.domain.certificate.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.likelion.cms.domain.certificate.entity.ActivityCertificate;
import com.likelion.cms.domain.certificate.entity.CertificateIssueStatus;
import com.likelion.cms.domain.cohort.dto.response.CohortSummary;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CertificateResponse {

    private final Long certificateId;
    private final CertificateIssueStatus status;
    private final CertificatePreviewResponse snapshot;
    private final LocalDateTime issuedAt;
    private final LocalDateTime createdAt;

    public static CertificateResponse of(Long certificateId, CertificateIssueStatus status,
                                         CertificatePreviewResponse snapshot,
                                         LocalDateTime issuedAt, LocalDateTime createdAt) {
        return new CertificateResponse(certificateId, status, snapshot, issuedAt, createdAt);
    }

    public static CertificateResponse from(ActivityCertificate certificate) {
        CohortSummary cohortSummary = CohortSummary.of(
                certificate.getCohort().getCohortId(),
                certificate.getCohort().getNumber(),
                certificate.getCohort().getName()
        );
        CertificatePreviewResponse snapshot = CertificatePreviewResponse.of(
                certificate.getNameSnapshot(),
                certificate.getDepartmentSnapshot(),
                certificate.getStudentIdSnapshot(),
                cohortSummary,
                certificate.getPartSnapshot(),
                certificate.getActivityStartedAt(),
                certificate.getActivityEndedAt()
        );
        return of(
                certificate.getCertificateId(),
                certificate.getIssueStatus(),
                snapshot,
                certificate.getIssuedAt(),
                certificate.getCreatedAt()
        );
    }
}