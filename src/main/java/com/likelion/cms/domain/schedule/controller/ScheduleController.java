package com.likelion.cms.domain.schedule.controller;

import com.likelion.cms.domain.schedule.dto.response.ScheduleResponse;
import com.likelion.cms.domain.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping
    public ResponseEntity<List<ScheduleResponse>> getSchedules(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth,
            @RequestParam(required = false) Long cohortId) {
        return ResponseEntity.ok(scheduleService.getSchedules(yearMonth, cohortId));
    }

    @GetMapping("/{scheduleId}")
    public ResponseEntity<ScheduleResponse> getSchedule(@PathVariable Long scheduleId) {
        return ResponseEntity.ok(scheduleService.getSchedule(scheduleId));
    }
}