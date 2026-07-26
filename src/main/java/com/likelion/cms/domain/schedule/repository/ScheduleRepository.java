package com.likelion.cms.domain.schedule.repository;

import com.likelion.cms.domain.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    List<Schedule> findByScheduleDateBetweenOrderByIsAllDayDescStartTimeAscTitleAsc(
            LocalDate start, LocalDate end);

    List<Schedule> findByScheduleDateBetweenAndCohort_CohortIdOrderByIsAllDayDescStartTimeAscTitleAsc(
            LocalDate start, LocalDate end, Long cohortId);
}