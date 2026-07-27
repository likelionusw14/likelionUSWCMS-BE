package com.likelion.cms.support.audit.entity;

import com.likelion.cms.domain.user.entity.AppUser;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 관리자 감사 로그.
 * <p>불변 로그로 updatedAt이 없으며 BaseTimeEntity를 상속하지 않는다.</p>
 */
@Entity
@Table(name = "AdminAuditLog", indexes = {
        @Index(name = "idx_audit_actor", columnList = "actorUserId"),
        @Index(name = "idx_audit_resource", columnList = "resourceType, resourceId"),
        @Index(name = "idx_audit_created_at", columnList = "createdAt"),
        @Index(name = "idx_audit_request_id", columnList = "requestId")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long auditLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actorUserId", nullable = false)
    private AppUser actorUser;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(nullable = false, length = 100)
    private String resourceType;

    @Column(nullable = false, length = 100)
    private String resourceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private String beforeData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private String afterData;

    @Column(length = 1000)
    private String reason;

    @Column(length = 100)
    private String requestId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private AdminAuditLog(AppUser actorUser, String action, String resourceType,
                          String resourceId, String beforeData, String afterData,
                          String reason, String requestId) {
        this.actorUser = actorUser;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.beforeData = beforeData;
        this.afterData = afterData;
        this.reason = reason;
        this.requestId = requestId;
        this.createdAt = LocalDateTime.now();
    }
}
