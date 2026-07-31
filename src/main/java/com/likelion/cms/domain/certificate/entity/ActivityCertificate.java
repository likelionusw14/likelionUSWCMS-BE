package com.likelion.cms.domain.certificate.entity;

import com.likelion.cms.common.type.PartType;
import com.likelion.cms.domain.cohort.entity.Cohort;
import com.likelion.cms.domain.user.entity.AppUser;
import com.likelion.cms.global.entity.BaseTimeEntity;
import com.likelion.cms.support.file.entity.FileAsset;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ActivityCertificate", indexes = {
        @Index(name = "idx_certificate_user_id", columnList = "userId"),
        @Index(name = "idx_certificate_cohort_id", columnList = "cohortId"),
        @Index(name = "idx_certificate_file_asset", columnList = "fileAssetId"),
        @Index(name = "idx_certificate_issue_status", columnList = "issueStatus"),
        @Index(name = "idx_certificate_user_created", columnList = "userId, createdAt"),
        @Index(name = "idx_certificate_idempotency_key", columnList = "idempotencyKey", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityCertificate extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long certificateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cohortId", nullable = false)
    private Cohort cohort;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fileAssetId")
    private FileAsset fileAsset;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CertificateIssueStatus issueStatus;

    @Column(nullable = false, length = 50)
    private String nameSnapshot;

    @Column(nullable = false, length = 100)
    private String departmentSnapshot;

    @Column(nullable = false, length = 30)
    private String studentIdSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartType partSnapshot;

    private LocalDate activityStartedAt;

    private LocalDate activityEndedAt;

    private LocalDateTime issuedAt;

    @Column(length = 1000)
    private String failureReason;

    @Column(unique = true)
    private UUID idempotencyKey;

    @Builder
    private ActivityCertificate(AppUser user, Cohort cohort, FileAsset fileAsset,
                                CertificateIssueStatus issueStatus, String nameSnapshot,
                                String departmentSnapshot, String studentIdSnapshot,
                                PartType partSnapshot, LocalDate activityStartedAt,
                                LocalDate activityEndedAt, LocalDateTime issuedAt,
                                String failureReason, UUID idempotencyKey) {
        this.user = user;
        this.cohort = cohort;
        this.fileAsset = fileAsset;
        this.issueStatus = issueStatus;
        this.nameSnapshot = nameSnapshot;
        this.departmentSnapshot = departmentSnapshot;
        this.studentIdSnapshot = studentIdSnapshot;
        this.partSnapshot = partSnapshot;
        this.activityStartedAt = activityStartedAt;
        this.activityEndedAt = activityEndedAt;
        this.issuedAt = issuedAt;
        this.failureReason = failureReason;
        this.idempotencyKey = idempotencyKey;
    }
}