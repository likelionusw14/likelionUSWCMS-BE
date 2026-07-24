package com.likelion.cms.domain.schedule.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private CohortRepository cohortRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private ScheduleService scheduleService;

    private Cohort cohort;
    private AppUser actor;

    @BeforeEach
    void setUp() {
        cohort = mock(Cohort.class);
        lenient().when(cohort.getCohortId()).thenReturn(1L);
        lenient().when(cohort.getNumber()).thenReturn(14);
        lenient().when(cohort.getName()).thenReturn("14기");

        actor = mock(AppUser.class);
        lenient().when(actor.getUserId()).thenReturn(100L);
    }

    @Test
    @DisplayName("정상 요청이면 일정이 등록된다")
    void create_success() {
        CreateScheduleRequest request = new CreateScheduleRequest(
                "정기 세션", "설명", 1L, LocalDate.of(2026, 8, 1),
                false, LocalTime.of(19, 0), "미래 101호"
        );

        Schedule saved = Schedule.builder()
                .title(request.title())
                .description(request.description())
                .cohort(cohort)
                .scheduleDate(request.scheduleDate())
                .isAllDay(request.isAllDay())
                .startTime(request.startTime())
                .location(request.location())
                .createdByUser(actor)
                .build();
        ReflectionTestUtils.setField(saved, "scheduleId", 10L);
        ReflectionTestUtils.setField(saved, "version", 0);

        when(appUserRepository.findById(100L)).thenReturn(Optional.of(actor));
        when(cohortRepository.findById(1L)).thenReturn(Optional.of(cohort));
        when(scheduleRepository.save(any(Schedule.class))).thenReturn(saved);

        ScheduleResponse response = scheduleService.create(request, 100L);

        assertThat(response.getScheduleId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("정기 세션");
        assertThat(response.getCohort().getCohortId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("존재하지 않는 cohortId면 RESOURCE_NOT_FOUND 예외가 발생한다")
    void create_cohortNotFound() {
        CreateScheduleRequest request = new CreateScheduleRequest(
                "정기 세션", null, 999L, LocalDate.of(2026, 8, 1),
                false, LocalTime.of(19, 0), null
        );

        when(appUserRepository.findById(100L)).thenReturn(Optional.of(actor));
        when(cohortRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleService.create(request, 100L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 actorUserId면 UNAUTHORIZED 예외가 발생한다")
    void create_actorNotFound() {
        CreateScheduleRequest request = new CreateScheduleRequest(
                "정기 세션", null, 1L, LocalDate.of(2026, 8, 1),
                false, LocalTime.of(19, 0), null
        );

        when(appUserRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleService.create(request, 999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("isAllDay=true인데 startTime이 있으면 INVALID_INPUT 예외가 발생한다")
    void create_allDayWithStartTime_fails() {
        CreateScheduleRequest request = new CreateScheduleRequest(
                "정기 세션", null, 1L, LocalDate.of(2026, 8, 1),
                true, LocalTime.of(19, 0), null
        );

        when(appUserRepository.findById(100L)).thenReturn(Optional.of(actor));
        when(cohortRepository.findById(1L)).thenReturn(Optional.of(cohort));

        assertThatThrownBy(() -> scheduleService.create(request, 100L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("isAllDay=false인데 startTime이 없으면 INVALID_INPUT 예외가 발생한다")
    void create_notAllDayWithoutStartTime_fails() {
        CreateScheduleRequest request = new CreateScheduleRequest(
                "정기 세션", null, 1L, LocalDate.of(2026, 8, 1),
                false, null, null
        );

        when(appUserRepository.findById(100L)).thenReturn(Optional.of(actor));
        when(cohortRepository.findById(1L)).thenReturn(Optional.of(cohort));

        assertThatThrownBy(() -> scheduleService.create(request, 100L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    private Schedule existingSchedule(boolean allDay, LocalTime startTime) {
        Schedule schedule = Schedule.builder()
                .title("기존 제목")
                .description("기존 설명")
                .cohort(cohort)
                .scheduleDate(LocalDate.of(2026, 8, 1))
                .isAllDay(allDay)
                .startTime(startTime)
                .location("기존 장소")
                .createdByUser(actor)
                .build();
        ReflectionTestUtils.setField(schedule, "scheduleId", 10L);
        ReflectionTestUtils.setField(schedule, "version", 1);
        return schedule;
    }

    private UpdateScheduleRequest fromJson(String json) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper.readValue(json, UpdateScheduleRequest.class);
    }

    @Test
    @DisplayName("버전이 일치하고 일부 필드만 수정 요청하면 나머지는 기존 값이 유지된다")
    void update_partialUpdate_keepsUnprovidedFields() throws Exception {
        Schedule schedule = existingSchedule(false, LocalTime.of(19, 0));
        UpdateScheduleRequest request = fromJson("""
                {"version": 1, "title": "변경된 제목"}
                """);

        when(appUserRepository.findById(100L)).thenReturn(Optional.of(actor));
        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));

        ScheduleResponse response = scheduleService.update(10L, request, 100L);

        assertThat(response.getTitle()).isEqualTo("변경된 제목");
        assertThat(response.getDescription()).isEqualTo("기존 설명");
        assertThat(response.getLocation()).isEqualTo("기존 장소");
        assertThat(response.getStartTime()).isEqualTo(LocalTime.of(19, 0));
    }

    @Test
    @DisplayName("version이 일치하지 않으면 OPTIMISTIC_LOCK_CONFLICT 예외가 발생한다")
    void update_versionMismatch_fails() throws Exception {
        Schedule schedule = existingSchedule(false, LocalTime.of(19, 0));
        UpdateScheduleRequest request = fromJson("""
                {"version": 999, "title": "변경된 제목"}
                """);

        when(appUserRepository.findById(100L)).thenReturn(Optional.of(actor));
        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));

        assertThatThrownBy(() -> scheduleService.update(10L, request, 100L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.OPTIMISTIC_LOCK_CONFLICT);
    }

    @Test
    @DisplayName("존재하지 않는 scheduleId면 RESOURCE_NOT_FOUND 예외가 발생한다")
    void update_scheduleNotFound_fails() throws Exception {
        UpdateScheduleRequest request = fromJson("""
                {"version": 1, "title": "변경된 제목"}
                """);

        when(appUserRepository.findById(100L)).thenReturn(Optional.of(actor));
        when(scheduleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleService.update(999L, request, 100L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("isAllDay를 true로 변경하면 기존 startTime이 제거된다")
    void update_changeToAllDay_clearsStartTime() throws Exception {
        Schedule schedule = existingSchedule(false, LocalTime.of(19, 0));
        UpdateScheduleRequest request = fromJson("""
                {"version": 1, "isAllDay": true}
                """);

        when(appUserRepository.findById(100L)).thenReturn(Optional.of(actor));
        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));

        ScheduleResponse response = scheduleService.update(10L, request, 100L);

        assertThat(response.getIsAllDay()).isTrue();
        assertThat(response.getStartTime()).isNull();
    }

    @Test
    @DisplayName("이미 종일 일정인 상태에서 startTime만 보내면 INVALID_INPUT 예외가 발생한다 (조용히 무시되지 않음)")
    void update_alreadyAllDay_withStartTimeOnly_fails() throws Exception {
        Schedule schedule = existingSchedule(true, null);
        UpdateScheduleRequest request = fromJson("""
                {"version": 1, "startTime": "19:00:00"}
                """);

        when(appUserRepository.findById(100L)).thenReturn(Optional.of(actor));
        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));

        assertThatThrownBy(() -> scheduleService.update(10L, request, 100L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("종일 일정을 시간 일정으로 바꾸면서 startTime을 안 보내면 INVALID_INPUT 예외가 발생한다")
    void update_changeToTimedWithoutStartTime_fails() throws Exception {
        Schedule schedule = existingSchedule(true, null);
        UpdateScheduleRequest request = fromJson("""
                {"version": 1, "isAllDay": false}
                """);

        when(appUserRepository.findById(100L)).thenReturn(Optional.of(actor));
        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));

        assertThatThrownBy(() -> scheduleService.update(10L, request, 100L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("title에 앞뒤 공백이 있으면 trim되어 저장된다")
    void update_titleIsTrimmed() throws Exception {
        Schedule schedule = existingSchedule(false, LocalTime.of(19, 0));
        UpdateScheduleRequest request = fromJson("""
                {"version": 1, "title": "  변경된 제목  "}
                """);

        when(appUserRepository.findById(100L)).thenReturn(Optional.of(actor));
        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));

        ScheduleResponse response = scheduleService.update(10L, request, 100L);

        assertThat(response.getTitle()).isEqualTo("변경된 제목");
    }

    @Test
    @DisplayName("description을 명시적으로 null로 보내면 NPE 없이 정상적으로 지워진다")
    void update_explicitNullDescription_clearsWithoutError() throws Exception {
        Schedule schedule = existingSchedule(false, LocalTime.of(19, 0));
        UpdateScheduleRequest request = fromJson("""
                {"version": 1, "description": null}
                """);

        when(appUserRepository.findById(100L)).thenReturn(Optional.of(actor));
        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));

        ScheduleResponse response = scheduleService.update(10L, request, 100L);

        assertThat(response.getDescription()).isNull();
    }

    @Test
    @DisplayName("정상 삭제 요청이면 softDelete가 호출된다")
    void delete_success() {
        Schedule schedule = existingSchedule(false, LocalTime.of(19, 0));

        when(appUserRepository.findById(100L)).thenReturn(Optional.of(actor));
        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));

        scheduleService.delete(10L, 100L);

        assertThat(schedule.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 scheduleId 삭제 요청이면 RESOURCE_NOT_FOUND 예외가 발생한다")
    void delete_notFound_fails() {
        when(appUserRepository.findById(100L)).thenReturn(Optional.of(actor));
        when(scheduleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleService.delete(999L, 100L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }
}