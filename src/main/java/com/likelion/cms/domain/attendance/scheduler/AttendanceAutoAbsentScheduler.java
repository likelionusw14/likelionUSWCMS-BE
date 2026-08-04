package com.likelion.cms.domain.attendance.scheduler;

import java.time.LocalDateTime;

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

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void convertExpiredNotCheckedToAbsent() {
        LocalDateTime cutoff = LocalDateTime.now().minus(AttendanceCodeService.CODE_TTL);

        int updated = attendanceRepository.bulkUpdateExpiredStatus(
                AttendanceStatus.NOT_CHECKED, AttendanceStatus.ABSENT, cutoff);

        if (updated > 0) {
            log.info("코드 발급 후 {}분 경과 미체크인 {}건을 ABSENT로 자동 전환했습니다.",
                    AttendanceCodeService.CODE_TTL.toMinutes(), updated);
        }
    }
}