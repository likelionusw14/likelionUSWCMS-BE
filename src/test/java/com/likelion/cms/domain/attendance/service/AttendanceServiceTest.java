package com.likelion.cms.domain.attendance.service;

import com.likelion.cms.common.type.PartType;
import com.likelion.cms.domain.attendance.dto.response.AttendanceResponse;
import com.likelion.cms.domain.attendance.entity.Attendance;
import com.likelion.cms.domain.attendance.entity.AttendanceStatus;
import com.likelion.cms.domain.attendance.entity.CheckInSource;
import com.likelion.cms.domain.attendance.repository.AttendanceRepository;
import com.likelion.cms.domain.schedule.entity.Schedule;
import com.likelion.cms.domain.user.entity.AppUser;
import com.likelion.cms.global.exception.BusinessException;
import com.likelion.cms.global.exception.ErrorCode;
import com.likelion.cms.global.response.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private AttendanceCodeService attendanceCodeService;

    private AttendanceService attendanceService;

    @BeforeEach
    void setUp() {
        attendanceService = new AttendanceService(attendanceRepository, attendanceCodeService);
    }

    @Test
    void checkInMarksNotCheckedAttendanceAsPresentWhenCodeMatches() {
        Attendance attendance = attendance(AttendanceStatus.NOT_CHECKED);
        when(attendanceRepository.findByUser_UserIdAndSchedule_ScheduleId(1L, 10L))
                .thenReturn(Optional.of(attendance));
        when(attendanceCodeService.matches(10L, "123456")).thenReturn(true);

        AttendanceResponse response = attendanceService.checkIn(1L, 10L, "123456");

        assertThat(response.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(attendance.getCheckInSource()).isEqualTo(CheckInSource.SELF_CODE);
        assertThat(attendance.getCheckedAt()).isNotNull();
    }

    @Test
    void checkInIsIdempotentWhenAlreadyPresent() {
        Attendance attendance = attendance(AttendanceStatus.PRESENT);
        when(attendanceRepository.findByUser_UserIdAndSchedule_ScheduleId(1L, 10L))
                .thenReturn(Optional.of(attendance));

        AttendanceResponse response = attendanceService.checkIn(1L, 10L, "123456");

        assertThat(response.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
        verify(attendanceCodeService, never()).matches(10L, "123456");
    }

    @Test
    void checkInRejectsAlreadyFinalizedAttendance() {
        Attendance attendance = attendance(AttendanceStatus.ABSENT);
        when(attendanceRepository.findByUser_UserIdAndSchedule_ScheduleId(1L, 10L))
                .thenReturn(Optional.of(attendance));

        assertThatThrownBy(() -> attendanceService.checkIn(1L, 10L, "123456"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ATTENDANCE_ALREADY_FINALIZED));
        verify(attendanceCodeService, never()).matches(10L, "123456");
    }

    @Test
    void checkInRejectsInvalidCode() {
        Attendance attendance = attendance(AttendanceStatus.NOT_CHECKED);
        when(attendanceRepository.findByUser_UserIdAndSchedule_ScheduleId(1L, 10L))
                .thenReturn(Optional.of(attendance));
        when(attendanceCodeService.matches(10L, "000000")).thenReturn(false);

        assertThatThrownBy(() -> attendanceService.checkIn(1L, 10L, "000000"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ATTENDANCE_CODE_INVALID));
        assertThat(attendance.getStatus()).isEqualTo(AttendanceStatus.NOT_CHECKED);
    }

    @Test
    void checkInRejectsMissingAttendanceRecord() {
        when(attendanceRepository.findByUser_UserIdAndSchedule_ScheduleId(1L, 10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> attendanceService.checkIn(1L, 10L, "123456"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void listMineReturnsPagedAttendances() {
        Attendance attendance = attendance(AttendanceStatus.PRESENT);
        Page<Attendance> page = new PageImpl<>(List.of(attendance), PageRequest.of(0, 20), 1);
        when(attendanceRepository.findAllByUserId(1L, PageRequest.of(0, 20))).thenReturn(page);

        PageResponse<AttendanceResponse> response = attendanceService.listMine(1L, 0, 20);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getPage().getTotalElements()).isEqualTo(1);
    }

    private Attendance attendance(AttendanceStatus status) {
        AppUser user = mock(AppUser.class);
        lenient().when(user.getUserId()).thenReturn(1L);
        lenient().when(user.getName()).thenReturn("홍길동");
        lenient().when(user.getPart()).thenReturn(PartType.BACKEND);

        Schedule schedule = mock(Schedule.class);
        lenient().when(schedule.getScheduleId()).thenReturn(10L);
        lenient().when(schedule.getTitle()).thenReturn("정기 세션");
        lenient().when(schedule.getScheduleDate()).thenReturn(LocalDate.of(2026, 7, 20));

        Attendance attendance = Attendance.builder()
                .user(user)
                .schedule(schedule)
                .status(status)
                .build();
        ReflectionTestUtils.setField(attendance, "attendanceId", 100L);
        ReflectionTestUtils.setField(attendance, "version", 0);
        ReflectionTestUtils.setField(attendance, "createdAt", LocalDateTime.of(2026, 7, 20, 10, 0));
        ReflectionTestUtils.setField(attendance, "updatedAt", LocalDateTime.of(2026, 7, 20, 10, 0));
        return attendance;
    }
}
