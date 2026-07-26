package com.likelion.cms.domain.schedule.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record CreateScheduleRequest(
        @NotBlank @Size(max = 150) String title,
        @Size(max = 10000)String description,
        @NotNull Long cohortId,
        @NotNull LocalDate scheduleDate,
        @NotNull Boolean isAllDay,
        LocalTime startTime,
        @Size(max = 255) String location
){
}
