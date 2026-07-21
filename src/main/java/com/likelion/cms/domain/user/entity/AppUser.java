package com.likelion.cms.domain.user.entity;

import com.likelion.cms.common.type.PartType;
import com.likelion.cms.domain.cohort.entity.Cohort;
import com.likelion.cms.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "AppUser", indexes = {
        @Index(name = "idx_app_user_cohort_id", columnList = "cohortId"),
        @Index(name = "idx_app_user_approved_by", columnList = "approvedBy"),
        @Index(name = "idx_app_user_rejected_by", columnList = "rejectedBy"),
        @Index(name = "idx_app_user_account_status", columnList = "accountStatus"),
        @Index(name = "idx_app_user_system_role", columnList = "systemRole"),
        @Index(name = "idx_app_user_cohort_part", columnList = "cohortId, part"),
        @Index(name = "idx_app_user_status_created", columnList = "accountStatus, createdAt")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deletedAt IS NULL")
public class AppUser extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String kakaoSubject;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(nullable = false, unique = true, length = 30)
    private String studentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cohortId", nullable = false)
    private Cohort cohort;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartType part;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SystemRole systemRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus accountStatus;

    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approvedBy")
    private AppUser approvedByUser;

    private LocalDateTime rejectedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rejectedBy")
    private AppUser rejectedByUser;

    @Column(length = 500)
    private String rejectionReason;

    @Version
    @Column(nullable = false)
    private Integer version;

    private LocalDateTime deletedAt;

    @Builder
    private AppUser(String kakaoSubject, String name, String department, String studentId,
                    Cohort cohort, PartType part, SystemRole systemRole, AccountStatus accountStatus) {
        this.kakaoSubject = kakaoSubject;
        this.name = name;
        this.department = department;
        this.studentId = studentId;
        this.cohort = cohort;
        this.part = part;
        this.systemRole = systemRole != null ? systemRole : SystemRole.MEMBER;
        this.accountStatus = accountStatus != null ? accountStatus : AccountStatus.PENDING;
    }

    public void approve(AppUser approver) {
        this.accountStatus = AccountStatus.ACTIVE;
        this.approvedByUser = approver;
        this.approvedAt = LocalDateTime.now();
    }

    public void reject(AppUser rejecter, String rejectionReason) {
        this.accountStatus = AccountStatus.REJECTED;
        this.rejectedByUser = rejecter;
        this.rejectedAt = LocalDateTime.now();
        this.rejectionReason = rejectionReason;
    }

    public void changeRole(SystemRole systemRole) {
        this.systemRole = systemRole;
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updateDepartment(String department) {
        this.department = department;
    }

    public void updatePart(PartType part) {
        this.part = part;
    }

    public void updateCohort(Cohort cohort) {
        this.cohort = cohort;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
