package com.likelion.cms.domain.attendance.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.likelion.cms.domain.attendance.entity.AttendanceStatus;
import com.likelion.cms.domain.attendance.repository.AttendanceRepository;
import com.likelion.cms.domain.attendance.service.AttendanceCodeCacheValue;
import com.likelion.cms.domain.attendance.service.AttendanceCodeService;

@ExtendWith(MockitoExtension.class)
class AttendanceAutoAbsentSchedulerTest {

    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private AttendanceCodeService attendanceCodeService;

    @InjectMocks
    private AttendanceAutoAbsentScheduler scheduler;

    @Test
    @DisplayName("코드가 아직 살아있는 날짜는 건드리지 않는다")
    void skipsDateWhenCodeStillActive() {
        LocalDate activeDate = LocalDate.of(2026, 8, 4);

        when(attendanceRepository.findDistinctAttendanceDatesByStatus(AttendanceStatus.NOT_CHECKED))
                .thenReturn(List.of(activeDate));
        when(attendanceCodeService.getCurrent(activeDate))
                .thenReturn(Optional.of(new AttendanceCodeCacheValue("123456", LocalDateTime.now())));

        scheduler.convertExpiredNotCheckedToAbsent();

        verify(attendanceRepository, never())
                .bulkUpdateStatus(eq(activeDate), any(AttendanceStatus.class), any(AttendanceStatus.class));
    }

    @Test
    @DisplayName("코드가 만료된 날짜는 NOT_CHECKED를 ABSENT로 일괄 전환한다")
    void convertsExpiredDateToAbsent() {
        LocalDate expiredDate = LocalDate.of(2026, 8, 3);

        when(attendanceRepository.findDistinctAttendanceDatesByStatus(AttendanceStatus.NOT_CHECKED))
                .thenReturn(List.of(expiredDate));
        when(attendanceCodeService.getCurrent(expiredDate))
                .thenReturn(Optional.empty());
        when(attendanceRepository.bulkUpdateStatus(
                expiredDate, AttendanceStatus.NOT_CHECKED, AttendanceStatus.ABSENT))
                .thenReturn(3);

        scheduler.convertExpiredNotCheckedToAbsent();

        verify(attendanceRepository, times(1))
                .bulkUpdateStatus(expiredDate, AttendanceStatus.NOT_CHECKED, AttendanceStatus.ABSENT);
    }

    @Test
    @DisplayName("NOT_CHECKED가 남은 날짜가 없으면 아무 것도 하지 않는다")
    void doesNothingWhenNoPendingDates() {
        when(attendanceRepository.findDistinctAttendanceDatesByStatus(AttendanceStatus.NOT_CHECKED))
                .thenReturn(List.of());

        scheduler.convertExpiredNotCheckedToAbsent();

        verify(attendanceCodeService, never()).getCurrent(any(LocalDate.class));
        verify(attendanceRepository, never())
                .bulkUpdateStatus(any(LocalDate.class), any(AttendanceStatus.class), any(AttendanceStatus.class));
    }

    @Test
    @DisplayName("여러 날짜가 섞여 있으면 각 날짜를 독립적으로 판단한다")
    void handlesMultipleDatesIndependently() {
        LocalDate activeDate = LocalDate.of(2026, 8, 4);
        LocalDate expiredDate = LocalDate.of(2026, 8, 3);

        when(attendanceRepository.findDistinctAttendanceDatesByStatus(AttendanceStatus.NOT_CHECKED))
                .thenReturn(List.of(activeDate, expiredDate));
        when(attendanceCodeService.getCurrent(activeDate))
                .thenReturn(Optional.of(new AttendanceCodeCacheValue("111111", LocalDateTime.now())));
        when(attendanceCodeService.getCurrent(expiredDate))
                .thenReturn(Optional.empty());
        when(attendanceRepository.bulkUpdateStatus(
                expiredDate, AttendanceStatus.NOT_CHECKED, AttendanceStatus.ABSENT))
                .thenReturn(1);

        scheduler.convertExpiredNotCheckedToAbsent();

        verify(attendanceRepository, never())
                .bulkUpdateStatus(eq(activeDate), any(AttendanceStatus.class), any(AttendanceStatus.class));
        verify(attendanceRepository, times(1))
                .bulkUpdateStatus(expiredDate, AttendanceStatus.NOT_CHECKED, AttendanceStatus.ABSENT);
    }
}