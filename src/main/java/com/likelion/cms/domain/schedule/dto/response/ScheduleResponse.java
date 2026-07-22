package com.likelion.cms.domain.schedule.dto.response;

import com.likelion.cms.domain.cohort.entity.Cohort;
import com.likelion.cms.domain.schedule.entity.Schedule;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ScheduleResponse(
        Long scheduleId,
        String title,
        String description,
        CohortSummaryResponse cohort,
        LocalDate scheduleDate,
        Boolean isAllDay,
        LocalTime startTime,
        String location,
        Integer version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ScheduleResponse from(Schedule schedule) {
        Cohort cohort = schedule.getCohort();
        return new ScheduleResponse(
                schedule.getScheduleId(),
                schedule.getTitle(),
                schedule.getDescription(),
                new CohortSummaryResponse(cohort.getCohortId(), cohort.getNumber(), cohort.getName()),
                schedule.getScheduleDate(),
                schedule.getIsAllDay(),
                schedule.getStartTime(),
                schedule.getLocation(),
                schedule.getVersion(),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt()
        );
    }
}


