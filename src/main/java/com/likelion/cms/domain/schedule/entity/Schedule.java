package com.likelion.cms.domain.schedule.entity;

import com.likelion.cms.domain.cohort.entity.Cohort;
import com.likelion.cms.domain.user.entity.AppUser;
import com.likelion.cms.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.cglib.core.Local;

@Entity
@Table(name = "Schedule", indexes = {
        @Index(name = "idx_schedule_cohort_id", columnList = "cohortId"),
        @Index(name = "idx_schedule_created_by", columnList = "createdBy"),
        @Index(name = "idx_schedule_date", columnList = "scheduleDate"),
        @Index(name = "idx_schedule_cohort_date", columnList = "cohortId, scheduleDate"),
        @Index(name = "idx_schedule_date_sort", columnList = "scheduleDate, isAllDay, startTime, title")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deletedAt IS NULL")
public class Schedule extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long scheduleId;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cohortId", nullable = false)
    private Cohort cohort;

    @Column(nullable = false)
    private LocalDate scheduleDate;

    @Column(nullable = false)
    private Boolean isAllDay;

    private LocalTime startTime;

    @Column(length = 255)
    private String location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "createdBy", nullable = false)
    private AppUser createdByUser;

    @Version
    @Column(nullable = false)
    private Integer version;

    private LocalDateTime deletedAt;

    @Builder
    private Schedule(String title, String description, Cohort cohort, LocalDate scheduleDate,
                     Boolean isAllDay, LocalTime startTime, String location, AppUser createdByUser) {
        this.title = title;
        this.description = description;
        this.cohort = cohort;
        this.scheduleDate = scheduleDate;
        this.isAllDay = isAllDay != null ? isAllDay : false;
        this.startTime = startTime;
        this.location = location;
        this.createdByUser = createdByUser;
    }

    public void softDelete(){
        this.deletedAt = LocalDateTime.now();
    }

    public void update(String title, String description, LocalDate scheduleDate
            , Boolean isAllDay, LocalTime startTime, String location) {
        this.title = title;
        this.description = description;
        this.scheduleDate = scheduleDate;
        this.isAllDay = isAllDay;
        this.startTime = startTime;
        this.location = location;
    }
}
