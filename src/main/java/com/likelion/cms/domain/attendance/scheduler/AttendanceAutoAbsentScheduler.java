package com.likelion.cms.domain.attendance.scheduler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.likelion.cms.domain.attendance.entity.AttendanceStatus;
import com.likelion.cms.domain.attendance.repository.AttendanceRepository;
import com.likelion.cms.domain.attendance.service.AttendanceCodeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceAutoAbsentScheduler {

    private final AttendanceRepository attendanceRepository;
    private final AttendanceCodeService attendanceCodeService;

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void convertExpiredNotCheckedToAbsent() {
        List<LocalDate> pendingDates =
                attendanceRepository.findDistinctAttendanceDatesByStatus(AttendanceStatus.NOT_CHECKED);

        LocalDateTime now = LocalDateTime.now();

        for (LocalDate date : pendingDates) {
            boolean codeStillActive = attendanceCodeService.getCurrent(date).isPresent();
            if (codeStillActive) {
                continue;
            }

            int updated = attendanceRepository.bulkUpdateStatus(
                    date, AttendanceStatus.NOT_CHECKED, AttendanceStatus.ABSENT, now);

            if (updated > 0) {
                log.info("attendanceDate={} 미체크인 {}건을 ABSENT로 자동 전환했습니다.", date, updated);
            }
        }
    }
}