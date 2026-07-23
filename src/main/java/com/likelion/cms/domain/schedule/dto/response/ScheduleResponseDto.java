package com.likelion.cms.domain.schedule.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.likelion.cms.domain.schedule.entity.Schedule;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScheduleResponseDto {

    private Long scheduleId;
    private String title;
    private String description;
    private CohortSummaryDto cohort;
    private LocalDate scheduleDate;
    private Boolean isAllDay;
    private LocalTime startTime;
    private String location;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ScheduleResponseDto from(Schedule schedule) {
        return ScheduleResponseDto.builder()
                .scheduleId(schedule.getScheduleId())
                .title(schedule.getTitle())
                .description(schedule.getDescription())
                .cohort(CohortSummaryDto.from(schedule.getCohort()))
                .scheduleDate(schedule.getScheduleDate())
                .isAllDay(schedule.getIsAllDay())
                .startTime(schedule.getStartTime())
                .location(schedule.getLocation())
                .version(schedule.getVersion())
                .createdAt(schedule.getCreatedAt())
                .updatedAt(schedule.getUpdatedAt())
                .build();
    }
}