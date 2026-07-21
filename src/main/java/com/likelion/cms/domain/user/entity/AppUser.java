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

    // ===== 여기부터 이번 이슈(관리자 회원 관리)에서 추가한 도메인 메서드 =====
    // 원래 엔티티엔 생성자만 있고 상태를 바꾸는 메서드가 없어서, 필요한 것만 추가함.
    // Setter를 열어두지 않고 "무엇을 하는 변경인지" 이름이 드러나는 메서드로만 상태를 바꾸게 함.

    // 가입 승인: PENDING -> ACTIVE. 승인자·승인시각을 같이 기록.
    public void approve(AppUser approver) {
        this.accountStatus = AccountStatus.ACTIVE;
        this.approvedByUser = approver;
        this.approvedAt = LocalDateTime.now();
    }

    // 가입 거절: PENDING -> REJECTED. 거절 사유까지 필수로 받음.
    public void reject(AppUser rejecter, String rejectionReason) {
        this.accountStatus = AccountStatus.REJECTED;
        this.rejectedByUser = rejecter;
        this.rejectedAt = LocalDateTime.now();
        this.rejectionReason = rejectionReason;
    }

    // 권한 변경 (MEMBER <-> ADMIN). "본인 강등 방지" 같은 비즈니스 규칙은
    // 여기서 체크하지 않고 UserService에서 함 - 엔티티는 상태 변경 자체에만 집중.
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

    // 소프트 삭제: 실제로 행을 지우지 않고 deletedAt만 채움.
    // 클래스에 걸린 @SQLRestriction("deletedAt IS NULL") 덕분에
    // 이후 모든 조회(findById 포함)에서 이 행은 자동으로 제외됨.
    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
