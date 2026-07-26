package com.likelion.cms.domain.attendance.entity;

import com.likelion.cms.domain.schedule.entity.Schedule;
import com.likelion.cms.domain.user.entity.AppUser;
import com.likelion.cms.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "Attendance",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_attendance_user_schedule",
                columnNames = {"userId", "scheduleId"}),
        indexes = {
                @Index(name = "idx_attendance_schedule_id", columnList = "scheduleId"),
                @Index(name = "idx_attendance_updated_by", columnList = "updatedBy"),
                @Index(name = "idx_attendance_schedule_status", columnList = "scheduleId, status"),
                @Index(name = "idx_attendance_user_created", columnList = "userId, createdAt")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Attendance extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long attendanceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheduleId", nullable = false)
    private Schedule schedule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status;

    private LocalDateTime checkedAt;

    @Enumerated(EnumType.STRING)
    private CheckInSource checkInSource;

    @Column(length = 1000)
    private String memo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updatedBy")
    private AppUser updatedByUser;

    @Version
    @Column(nullable = false)
    private Integer version;

    @Builder
    private Attendance(AppUser user, Schedule schedule, AttendanceStatus status) {
        this.user = user;
        this.schedule = schedule;
        this.status = status != null ? status : AttendanceStatus.NOT_CHECKED;
    }

    public void updateByAdmin(AttendanceStatus status,
                              String memo,
                              boolean memoProvided,
                              AppUser admin,
                              LocalDateTime updatedAt) {
        this.status = status;
        if (memoProvided) {
            this.memo = memo;
        }
        this.checkInSource = CheckInSource.ADMIN;
        this.checkedAt = updatedAt;
        this.updatedByUser = admin;
    }
}
