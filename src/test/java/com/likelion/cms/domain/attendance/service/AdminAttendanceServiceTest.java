package com.likelion.cms.domain.attendance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.likelion.cms.common.type.PartType;
import com.likelion.cms.domain.attendance.entity.AdminSettableAttendanceStatus;
import com.likelion.cms.domain.attendance.dto.request.UpdateAttendanceRequest;
import com.likelion.cms.domain.attendance.dto.response.AttendanceCodeResponse;
import com.likelion.cms.domain.attendance.dto.response.AttendanceResponse;
import com.likelion.cms.domain.attendance.entity.Attendance;
import com.likelion.cms.domain.attendance.entity.AttendanceStatus;
import com.likelion.cms.domain.attendance.entity.CheckInSource;
import com.likelion.cms.domain.attendance.repository.AttendanceRepository;
import com.likelion.cms.domain.cohort.entity.Cohort;
import com.likelion.cms.domain.schedule.entity.Schedule;
import com.likelion.cms.domain.schedule.repository.ScheduleRepository;
import com.likelion.cms.domain.user.entity.AccountStatus;
import com.likelion.cms.domain.user.entity.AppUser;
import com.likelion.cms.domain.user.entity.SystemRole;
import com.likelion.cms.domain.user.repository.AppUserRepository;
import com.likelion.cms.global.exception.BusinessException;
import com.likelion.cms.global.exception.ErrorCode;
import com.likelion.cms.global.response.PageResponse;

@ExtendWith(MockitoExtension.class)
class AdminAttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private ScheduleRepository scheduleRepository;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private AttendanceCodeService attendanceCodeService;

    @InjectMocks
    private AdminAttendanceService adminAttendanceService;

    private AppUser member;
    private AppUser admin;
    private Schedule schedule;
    private Attendance attendance;

    @BeforeEach
    void setUp() {
        member = AppUser.builder()
                .kakaoSubject("kakao-member")
                .name("정소윤")
                .department("컴퓨터공학과")
                .studentId("2021000001")
                .part(PartType.BACKEND)
                .systemRole(SystemRole.MEMBER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(member, "userId", 1L);

        admin = AppUser.builder()
                .kakaoSubject("kakao-admin")
                .name("관리자")
                .department("컴퓨터공학과")
                .studentId("2020000001")
                .part(PartType.BACKEND)
                .systemRole(SystemRole.ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(admin, "userId", 99L);

        schedule = mock(Schedule.class);
        ReflectionTestUtils.setField(schedule, "scheduleId", 10L);

        attendance = Attendance.builder()
                .user(member)
                .schedule(schedule)
                .status(AttendanceStatus.NOT_CHECKED)
                .build();
        ReflectionTestUtils.setField(attendance, "attendanceId", 100L);
        ReflectionTestUtils.setField(attendance, "version", 0);
    }

    @Test
    @DisplayName("존재하지 않는 scheduleId면 목록 조회 시 RESOURCE_NOT_FOUND 예외가 발생한다")
    void listAttendances_scheduleNotFound_throwsException() {
        when(scheduleRepository.findById(10L)).thenReturn(Optional.empty());

        Pageable pageable = PageRequest.of(0, 20);

        assertThatThrownBy(() -> adminAttendanceService.listAttendances(10L, null, null, null, pageable))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("정상 조회 시 Repository 결과가 PageResponse로 올바르게 매핑된다")
    void listAttendances_returnsPageResponse() {
        when(schedule.getScheduleId()).thenReturn(10L);
        when(schedule.getTitle()).thenReturn("정기 세션");
        when(schedule.getScheduleDate()).thenReturn(LocalDate.of(2026, 7, 25));

        Pageable pageable = PageRequest.of(0, 20);
        Page<Attendance> page = new PageImpl<>(List.of(attendance), pageable, 1);

        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(attendanceRepository.searchForAdmin(10L, null, null, null, pageable)).thenReturn(page);

        PageResponse<AttendanceResponse> response =
                adminAttendanceService.listAttendances(10L, null, null, null, pageable);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getAttendanceId()).isEqualTo(100L);
        assertThat(response.getPage().getTotalElements()).isEqualTo(1);
        assertThat(response.getPage().getPage()).isEqualTo(0);
    }

    @Test
    @DisplayName("version이 일치하지 않으면 OPTIMISTIC_LOCK_CONFLICT 예외가 발생한다")
    void updateAttendance_versionMismatch_throwsException() {
        UpdateAttendanceRequest request = new UpdateAttendanceRequest();
        request.setStatus(AdminSettableAttendanceStatus.PRESENT);
        request.setVersion(999);

        when(attendanceRepository.findById(100L)).thenReturn(Optional.of(attendance));

        assertThatThrownBy(() -> adminAttendanceService.updateAttendance(100L, request, 99L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OPTIMISTIC_LOCK_CONFLICT);
    }

    @Test
    @DisplayName("존재하지 않는 attendanceId면 RESOURCE_NOT_FOUND 예외가 발생한다")
    void updateAttendance_notFound_throwsException() {
        UpdateAttendanceRequest request = new UpdateAttendanceRequest();
        request.setStatus(AdminSettableAttendanceStatus.PRESENT);
        request.setVersion(0);

        when(attendanceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminAttendanceService.updateAttendance(999L, request, 99L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("memo를 안 보내면(미전달) 기존 memo가 유지된다")
    void updateAttendance_memoNotProvided_keepsExistingMemo() {
        ReflectionTestUtils.setField(attendance, "memo", "기존 메모");

        UpdateAttendanceRequest request = new UpdateAttendanceRequest();
        request.setStatus(AdminSettableAttendanceStatus.PRESENT);
        request.setVersion(0);

        when(attendanceRepository.findById(100L)).thenReturn(Optional.of(attendance));
        when(appUserRepository.getReferenceById(99L)).thenReturn(admin);
        when(schedule.getScheduleId()).thenReturn(10L);
        when(schedule.getTitle()).thenReturn("정기 세션");
        when(schedule.getScheduleDate()).thenReturn(LocalDate.of(2026, 7, 25));

        AttendanceResponse response = adminAttendanceService.updateAttendance(100L, request, 99L);

        assertThat(response.getMemo()).isEqualTo("기존 메모");
        assertThat(response.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
    }

    @Test
    @DisplayName("memo를 null로 명시하면 메모가 삭제된다")
    void updateAttendance_memoExplicitNull_clearsMemo() {
        ReflectionTestUtils.setField(attendance, "memo", "기존 메모");

        UpdateAttendanceRequest request = new UpdateAttendanceRequest();
        request.setStatus(AdminSettableAttendanceStatus.ABSENT);
        request.setMemo(null);
        request.setVersion(0);

        when(attendanceRepository.findById(100L)).thenReturn(Optional.of(attendance));
        when(appUserRepository.getReferenceById(99L)).thenReturn(admin);
        when(schedule.getScheduleId()).thenReturn(10L);
        when(schedule.getTitle()).thenReturn("정기 세션");
        when(schedule.getScheduleDate()).thenReturn(LocalDate.of(2026, 7, 25));

        AttendanceResponse response = adminAttendanceService.updateAttendance(100L, request, 99L);

        assertThat(response.getMemo()).isNull();
        assertThat(response.getStatus()).isEqualTo(AttendanceStatus.ABSENT);
    }

    @Test
    @DisplayName("관리자가 수정하면 checkInSource=ADMIN, checkedAt이 현재 시각으로 갱신된다")
    void updateAttendance_setsAdminCheckInSourceAndCheckedAt() {
        UpdateAttendanceRequest request = new UpdateAttendanceRequest();
        request.setStatus(AdminSettableAttendanceStatus.LATE);
        request.setMemo("버스 지연");
        request.setVersion(0);

        when(attendanceRepository.findById(100L)).thenReturn(Optional.of(attendance));
        when(appUserRepository.getReferenceById(99L)).thenReturn(admin);
        when(schedule.getScheduleId()).thenReturn(10L);
        when(schedule.getTitle()).thenReturn("정기 세션");
        when(schedule.getScheduleDate()).thenReturn(LocalDate.of(2026, 7, 25));

        AttendanceResponse response = adminAttendanceService.updateAttendance(100L, request, 99L);

        assertThat(response.getCheckInSource()).isEqualTo(CheckInSource.ADMIN);
        assertThat(response.getCheckedAt()).isNotNull();
        assertThat(response.getMemo()).isEqualTo("버스 지연");
    }

    @Test
    @DisplayName("존재하지 않는 scheduleId면 코드 발급 시 RESOURCE_NOT_FOUND 예외가 발생한다")
    void createOrReissueCode_scheduleNotFound_throwsException() {
        when(scheduleRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminAttendanceService.createOrReissueCode(10L, 99L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("코드 발급 시 이미 Attendance가 있는 사용자는 건드리지 않고, 없는 사용자만 NOT_CHECKED로 생성한다")
    void createOrReissueCode_createsOnlyMissingAttendanceRows() {
        Cohort cohort = mock(Cohort.class);
        when(cohort.getCohortId()).thenReturn(5L);
        when(schedule.getCohort()).thenReturn(cohort);
        when(schedule.getScheduleId()).thenReturn(10L);

        AppUser alreadyChecked = AppUser.builder()
                .kakaoSubject("kakao-2")
                .name("이미체크")
                .department("컴퓨터공학과")
                .studentId("2021000002")
                .part(PartType.FRONTEND)
                .systemRole(SystemRole.MEMBER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(alreadyChecked, "userId", 2L);

        AppUser notYetChecked = AppUser.builder()
                .kakaoSubject("kakao-3")
                .name("미체크")
                .department("컴퓨터공학과")
                .studentId("2021000003")
                .part(PartType.DESIGN)
                .systemRole(SystemRole.MEMBER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(notYetChecked, "userId", 3L);

        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(appUserRepository.findAllByCohort_CohortIdAndSystemRoleAndAccountStatus(
                5L, SystemRole.MEMBER, AccountStatus.ACTIVE))
                .thenReturn(List.of(alreadyChecked, notYetChecked));
        when(attendanceRepository.findUserIdsByScheduleId(10L)).thenReturn(List.of(2L));
        when(attendanceCodeService.issue(10L))
                .thenReturn(new AttendanceCodeCacheValue("123456", LocalDateTime.of(2026, 7, 25, 10, 0)));

        AttendanceCodeResponse response = adminAttendanceService.createOrReissueCode(10L, 99L);

        ArgumentCaptor<List<Attendance>> captor = ArgumentCaptor.forClass(List.class);
        verify(attendanceRepository, times(1)).saveAll(captor.capture());
        List<Attendance> saved = captor.getValue();

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getUser()).isEqualTo(notYetChecked);
        assertThat(saved.get(0).getStatus()).isEqualTo(AttendanceStatus.NOT_CHECKED);
        assertThat(response.getCode()).isEqualTo("123456");
        assertThat(response.getExpiresAt()).isEqualTo(LocalDateTime.of(2026, 7, 25, 10, 5));
    }

    @Test
    @DisplayName("대상자 전원이 이미 Attendance를 가지고 있으면 saveAll을 호출하지 않는다")
    void createOrReissueCode_noMissingTargets_doesNotCallSaveAll() {
        Cohort cohort = mock(Cohort.class);
        when(cohort.getCohortId()).thenReturn(5L);
        when(schedule.getCohort()).thenReturn(cohort);
        when(schedule.getScheduleId()).thenReturn(10L);

        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(appUserRepository.findAllByCohort_CohortIdAndSystemRoleAndAccountStatus(
                5L, SystemRole.MEMBER, AccountStatus.ACTIVE))
                .thenReturn(List.of(member));
        when(attendanceRepository.findUserIdsByScheduleId(10L)).thenReturn(List.of(1L));
        when(attendanceCodeService.issue(10L))
                .thenReturn(new AttendanceCodeCacheValue("654321", LocalDateTime.now()));

        adminAttendanceService.createOrReissueCode(10L, 99L);

        verify(attendanceRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("존재하지 않는 scheduleId면 코드 조회 시 RESOURCE_NOT_FOUND 예외가 발생한다")
    void getCurrentCode_scheduleNotFound_throwsException() {
        when(scheduleRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminAttendanceService.getCurrentCode(10L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("현재 유효한 코드가 없으면 RESOURCE_NOT_FOUND 예외가 발생한다")
    void getCurrentCode_noActiveCode_throwsException() {
        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(attendanceCodeService.getCurrent(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminAttendanceService.getCurrentCode(10L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("현재 유효한 코드가 있으면 그대로 반환하고 expiresAt은 startedAt+300초다")
    void getCurrentCode_returnsCurrentCode() {
        LocalDateTime startedAt = LocalDateTime.of(2026, 7, 25, 14, 0);
        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(attendanceCodeService.getCurrent(10L))
                .thenReturn(Optional.of(new AttendanceCodeCacheValue("111222", startedAt)));

        AttendanceCodeResponse response = adminAttendanceService.getCurrentCode(10L);

        assertThat(response.getScheduleId()).isEqualTo(10L);
        assertThat(response.getCode()).isEqualTo("111222");
        assertThat(response.getStartedAt()).isEqualTo(startedAt);
        assertThat(response.getExpiresAt()).isEqualTo(startedAt.plusSeconds(300));
    }
}