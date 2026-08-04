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

/**
 * #88: Redis 코드 키 생존 여부로 판단하는 방식.
 *
 * 판정 기준이 "Redis에 그 날짜 코드가 있는지"뿐이라, 스케줄러 입장에서는
 * "최초 발급"과 "재발급"을 구분하지 않는다 (둘 다 getCurrent()가 값을
 * 반환하면 동일하게 스킵). 그래서 재발급 전용 시나리오를 별도 테스트로
 * 만들어도 skipsDateWhenCodeStillActive와 검증 내용이 완전히 겹쳐
 * 중복이 된다 (CodeRabbit 리뷰로 발견, 중복 테스트 제거).
 *
 * "재발급해도 오결석 처리 안 됨"이 실제로 보장되는 이유는 이 스케줄러가
 * createdAt을 아예 참조하지 않고 Redis 키 존재 여부만 보기 때문이며,
 * 그 설계 자체가 이 클래스의 테스트 대상이다. 재발급 시 기존 Attendance
 * 행이 새로 만들어지지 않고 재사용된다는 사실은
 * AdminAttendanceServiceTest(createOrReissueCode 관련 테스트)에서
 * 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class AttendanceAutoAbsentSchedulerTest {

    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private AttendanceCodeService attendanceCodeService;

    @InjectMocks
    private AttendanceAutoAbsentScheduler scheduler;

    @Test
    @DisplayName("코드가 아직 살아있는 날짜는 건드리지 않는다 (최초 발급/재발급 모두 동일하게 적용됨)")
    void skipsDateWhenCodeStillActive() {
        LocalDate activeDate = LocalDate.of(2026, 8, 4);

        when(attendanceRepository.findDistinctAttendanceDatesByStatus(AttendanceStatus.NOT_CHECKED))
                .thenReturn(List.of(activeDate));
        when(attendanceCodeService.getCurrent(activeDate))
                .thenReturn(Optional.of(new AttendanceCodeCacheValue("123456", LocalDateTime.now())));

        scheduler.convertExpiredNotCheckedToAbsent();

        verify(attendanceRepository, never())
                .bulkUpdateStatus(eq(activeDate), any(), any(), any());
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
                eq(expiredDate), eq(AttendanceStatus.NOT_CHECKED), eq(AttendanceStatus.ABSENT), any()))
                .thenReturn(3);

        scheduler.convertExpiredNotCheckedToAbsent();

        verify(attendanceRepository, times(1)).bulkUpdateStatus(
                eq(expiredDate), eq(AttendanceStatus.NOT_CHECKED), eq(AttendanceStatus.ABSENT), any());
    }

    @Test
    @DisplayName("NOT_CHECKED가 남은 날짜가 없으면 아무 것도 하지 않는다")
    void doesNothingWhenNoPendingDates() {
        when(attendanceRepository.findDistinctAttendanceDatesByStatus(AttendanceStatus.NOT_CHECKED))
                .thenReturn(List.of());

        scheduler.convertExpiredNotCheckedToAbsent();

        verify(attendanceCodeService, never()).getCurrent(any());
        verify(attendanceRepository, never()).bulkUpdateStatus(any(), any(), any(), any());
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
                eq(expiredDate), eq(AttendanceStatus.NOT_CHECKED), eq(AttendanceStatus.ABSENT), any()))
                .thenReturn(1);

        scheduler.convertExpiredNotCheckedToAbsent();

        verify(attendanceRepository, never())
                .bulkUpdateStatus(eq(activeDate), any(), any(), any());
        verify(attendanceRepository, times(1)).bulkUpdateStatus(
                eq(expiredDate), eq(AttendanceStatus.NOT_CHECKED), eq(AttendanceStatus.ABSENT), any());
    }
}