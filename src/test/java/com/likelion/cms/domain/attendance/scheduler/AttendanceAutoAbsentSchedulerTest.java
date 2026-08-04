package com.likelion.cms.domain.attendance.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.likelion.cms.domain.attendance.entity.AttendanceStatus;
import com.likelion.cms.domain.attendance.repository.AttendanceRepository;
import com.likelion.cms.domain.attendance.service.AttendanceCodeService;

/**
 * #88: Redis 기반 판단(레이스 컨디션 있었음)에서 createdAt 기준
 * 단일 벌크 쿼리 방식으로 변경한 뒤의 테스트. AttendanceCodeService
 * 의존성이 사라져서 더 이상 mock할 필요가 없어짐.
 */
@ExtendWith(MockitoExtension.class)
class AttendanceAutoAbsentSchedulerTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @InjectMocks
    private AttendanceAutoAbsentScheduler scheduler;

    @Test
    @DisplayName("NOT_CHECKED이면서 CODE_TTL(5분) 이전에 생성된 레코드를 ABSENT로 일괄 전환한다")
    void convertsExpiredNotCheckedRecordsToAbsent() {
        when(attendanceRepository.bulkUpdateExpiredStatus(
                eq(AttendanceStatus.NOT_CHECKED), eq(AttendanceStatus.ABSENT), any(LocalDateTime.class)))
                .thenReturn(3);

        scheduler.convertExpiredNotCheckedToAbsent();

        verify(attendanceRepository, times(1)).bulkUpdateExpiredStatus(
                eq(AttendanceStatus.NOT_CHECKED), eq(AttendanceStatus.ABSENT), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("cutoff 시각은 현재 시각에서 CODE_TTL(5분)만큼 뺀 값으로 계산된다")
    void computesCutoffUsingCodeTtl() {
        when(attendanceRepository.bulkUpdateExpiredStatus(
                eq(AttendanceStatus.NOT_CHECKED), eq(AttendanceStatus.ABSENT), any(LocalDateTime.class)))
                .thenReturn(0);

        LocalDateTime before = LocalDateTime.now().minus(AttendanceCodeService.CODE_TTL);
        scheduler.convertExpiredNotCheckedToAbsent();
        LocalDateTime after = LocalDateTime.now().minus(AttendanceCodeService.CODE_TTL);

        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(attendanceRepository).bulkUpdateExpiredStatus(
                eq(AttendanceStatus.NOT_CHECKED), eq(AttendanceStatus.ABSENT), cutoffCaptor.capture());

        LocalDateTime actualCutoff = cutoffCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(actualCutoff)
                .isAfterOrEqualTo(before)
                .isBeforeOrEqualTo(after);
    }

    @Test
    @DisplayName("전환 대상이 없으면(0건) 예외 없이 정상 종료한다")
    void doesNotThrowWhenNoRecordsUpdated() {
        when(attendanceRepository.bulkUpdateExpiredStatus(
                eq(AttendanceStatus.NOT_CHECKED), eq(AttendanceStatus.ABSENT), any(LocalDateTime.class)))
                .thenReturn(0);

        org.assertj.core.api.Assertions.assertThatCode(() -> scheduler.convertExpiredNotCheckedToAbsent())
                .doesNotThrowAnyException();
    }
}