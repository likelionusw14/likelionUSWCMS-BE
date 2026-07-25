package com.likelion.cms.domain.schedule.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.likelion.cms.domain.cohort.dto.response.CohortSummary;
import com.likelion.cms.domain.cohort.entity.Cohort;
import com.likelion.cms.domain.schedule.entity.Schedule;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScheduleResponse {

    private final Long scheduleId;
    private final String title;
    private final String description;
    private final CohortSummary cohort;
    private final LocalDate scheduleDate;
    private final Boolean isAllDay;
    private final LocalTime startTime;
    private final String location;
    private final Integer version;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static ScheduleResponse of(Long scheduleId, String title, String description,
                                      CohortSummary cohort, LocalDate scheduleDate,
                                      Boolean isAllDay, LocalTime startTime, String location,
                                      Integer version, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new ScheduleResponse(scheduleId, title, description, cohort, scheduleDate,
                isAllDay, startTime, location, version, createdAt, updatedAt);
    }

    public static ScheduleResponse from(Schedule schedule) {
        Cohort cohort = schedule.getCohort();
        CohortSummary cohortSummary = CohortSummary.of(
                cohort.getCohortId(), cohort.getNumber(), cohort.getName()
        );
        return of(
                schedule.getScheduleId(),
                schedule.getTitle(),
                schedule.getDescription(),
                cohortSummary,
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