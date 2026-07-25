package com.likelion.cms.domain.schedule.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
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

    private Long scheduleId;
    private String title;
    private String description;
    private CohortSummaryResponse cohort;
    private LocalDate scheduleDate;
    private Boolean isAllDay;
    private LocalTime startTime;
    private String location;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ScheduleResponse of(Long scheduleId, String title, String description,
                                      CohortSummaryResponse cohort, LocalDate scheduleDate,
                                      Boolean isAllDay, LocalTime startTime, String location,
                                      Integer version, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new ScheduleResponse(scheduleId, title, description, cohort, scheduleDate,
                isAllDay, startTime, location, version, createdAt, updatedAt);
    }

    public static ScheduleResponse from(Schedule schedule) {
        Cohort cohort = schedule.getCohort();
        return of(
                schedule.getScheduleId(),
                schedule.getTitle(),
                schedule.getDescription(),
                CohortSummaryResponse.from(cohort),
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


