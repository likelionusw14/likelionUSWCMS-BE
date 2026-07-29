package com.likelion.cms.domain.schedule.service;

import com.likelion.cms.domain.cohort.entity.Cohort;
import com.likelion.cms.domain.cohort.repository.CohortRepository;
import com.likelion.cms.domain.schedule.dto.request.CreateScheduleRequest;
import com.likelion.cms.domain.schedule.dto.request.UpdateScheduleRequest;
import com.likelion.cms.domain.schedule.dto.response.ScheduleResponse;
import com.likelion.cms.domain.schedule.entity.Schedule;
import com.likelion.cms.domain.schedule.repository.ScheduleRepository;
import com.likelion.cms.domain.user.entity.AppUser;
import com.likelion.cms.domain.user.repository.AppUserRepository;
import com.likelion.cms.global.exception.BusinessException;
import com.likelion.cms.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final CohortRepository cohortRepository;
    private final AppUserRepository appUserRepository;

    public List<ScheduleResponse> getSchedules(YearMonth yearMonth, Long cohortId) {
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        List<Schedule> schedules = (cohortId != null)
                ? scheduleRepository.findByScheduleDateBetweenAndCohort_CohortIdOrderByIsAllDayDescStartTimeAscTitleAsc(start, end, cohortId)
                : scheduleRepository.findByScheduleDateBetweenOrderByIsAllDayDescStartTimeAscTitleAsc(start, end);

        return schedules.stream()
                .map(ScheduleResponse::from)
                .collect(Collectors.toList());
    }

    public ScheduleResponse getSchedule(Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return ScheduleResponse.from(schedule);
    }

    @Transactional
    public ScheduleResponse create(CreateScheduleRequest request, Long actorUserId) {
        AppUser actor = findActor(actorUserId);
        Cohort cohort = findCohort(request.cohortId());
        validateAllDayCombination(request.isAllDay(), request.startTime());

        Schedule schedule = Schedule.builder()
                .title(request.title().trim())
                .description(request.description() == null ? null : request.description().trim())
                .cohort(cohort)
                .scheduleDate(request.scheduleDate())
                .isAllDay(request.isAllDay())
                .startTime(request.startTime())
                .location(request.location())
                .createdByUser(actor)
                .build();

        return ScheduleResponse.from(scheduleRepository.save(schedule));
    }

    @Transactional
    public ScheduleResponse update(Long scheduleId, UpdateScheduleRequest request, Long actorUserId) {
        findActor(actorUserId);
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        validateVersion(schedule.getVersion(), request.getVersion());

        String finalTitle = request.isTitleProvided() ? request.getTitle().trim() : schedule.getTitle();
        String finalDescription = request.isDescriptionProvided()
                ? (request.getDescription() == null ? null : request.getDescription().trim())
                : schedule.getDescription();
        LocalDate finalScheduleDate = request.isScheduleDateProvided() ? request.getScheduleDate() : schedule.getScheduleDate();
        Boolean finalIsAllDay = request.isAllDayProvided() ? request.getIsAllDay() : schedule.getIsAllDay();
        LocalTime finalStartTime = request.isStartTimeProvided() ? request.getStartTime() : schedule.getStartTime();
        String finalLocation = request.isLocationProvided() ? request.getLocation() : schedule.getLocation();

        if (request.isAllDayProvided() && Boolean.TRUE.equals(request.getIsAllDay())) {
            finalStartTime = null;
        }
        validateAllDayCombination(finalIsAllDay, finalStartTime);

        schedule.update(finalTitle, finalDescription, finalScheduleDate, finalIsAllDay, finalStartTime, finalLocation);

        // @Version은 실제 UPDATE(flush) 시점에 증가한다. flush 없이 응답을 만들면
        // 갱신 전 version이 내려가 클라이언트의 다음 수정이 낙관적 락 충돌을 낸다.
        scheduleRepository.flush();
        return ScheduleResponse.from(schedule);
    }

    @Transactional
    public void delete(Long scheduleId, Long actorUserId) {
        findActor(actorUserId);
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        schedule.softDelete();
    }

    private AppUser findActor(Long actorUserId) {
        return appUserRepository.findById(actorUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    private Cohort findCohort(Long cohortId) {
        return cohortRepository.findById(cohortId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private void validateVersion(Integer actualVersion, Integer requestedVersion) {
        if (!requestedVersion.equals(actualVersion)) {
            throw new BusinessException(ErrorCode.OPTIMISTIC_LOCK_CONFLICT);
        }
    }

    private void validateAllDayCombination(Boolean isAllDay, LocalTime startTime) {
        if (Boolean.TRUE.equals(isAllDay) && startTime != null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "종일 일정에는 시작 시간을 입력할 수 없습니다.");
        }
        if (Boolean.FALSE.equals(isAllDay) && startTime == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "종일 일정이 아니면 시작 시간은 필수입니다.");
        }
    }
}