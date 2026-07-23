package com.likelion.cms.domain.schedule.service;

import com.likelion.cms.domain.schedule.dto.response.ScheduleResponseDto;
import com.likelion.cms.domain.schedule.entity.Schedule;
import com.likelion.cms.domain.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;

    public List<ScheduleResponseDto> getSchedules(String yearMonth, Long cohortId) {
        YearMonth ym = YearMonth.parse(yearMonth);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<Schedule> schedules = (cohortId != null)
                ? scheduleRepository.findByScheduleDateBetweenAndCohort_CohortIdOrderByIsAllDayDescStartTimeAscTitleAsc(start, end, cohortId)
                : scheduleRepository.findByScheduleDateBetweenOrderByIsAllDayDescStartTimeAscTitleAsc(start, end);

        return schedules.stream()
                .map(ScheduleResponseDto::from)
                .collect(Collectors.toList());
    }

    public ScheduleResponseDto getSchedule(Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));
        return ScheduleResponseDto.from(schedule);
    }
}