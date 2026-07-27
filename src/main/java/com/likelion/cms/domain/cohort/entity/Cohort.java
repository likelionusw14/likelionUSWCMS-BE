package com.likelion.cms.domain.cohort.entity;

import com.likelion.cms.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "Cohort", indexes = {
        @Index(name = "idx_cohort_status", columnList = "status"),
        @Index(name = "idx_cohort_status_number", columnList = "status, number")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cohort extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cohortId;

    @Column(nullable = false, unique = true)
    private Integer number;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    private LocalDate startedAt;

    private LocalDate endedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CohortStatus status;

    @Builder
    private Cohort(Integer number, String name, LocalDate startedAt, LocalDate endedAt, CohortStatus status) {
        this.number = number;
        this.name = name;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.status = status != null ? status : CohortStatus.PLANNED;
    }
}
