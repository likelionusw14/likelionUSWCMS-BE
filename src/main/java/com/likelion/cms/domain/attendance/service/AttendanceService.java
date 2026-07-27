package com.likelion.cms.domain.attendance.service;

import com.likelion.cms.domain.attendance.dto.response.AttendanceResponse;
import com.likelion.cms.domain.attendance.entity.Attendance;
import com.likelion.cms.domain.attendance.entity.AttendanceStatus;
import com.likelion.cms.domain.attendance.entity.CheckInSource;
import com.likelion.cms.domain.attendance.repository.AttendanceRepository;
import com.likelion.cms.global.exception.BusinessException;
import com.likelion.cms.global.exception.ErrorCode;
import com.likelion.cms.global.response.PageMeta;
import com.likelion.cms.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final AttendanceCodeService attendanceCodeService;

    public PageResponse<AttendanceResponse> listMine(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Attendance> attendancePage = attendanceRepository.findAllByUserId(userId, pageable);

        List<AttendanceResponse> items = attendancePage.getContent().stream()
                .map(AttendanceResponse::from)
                .toList();

        PageMeta pageMeta = PageMeta.of(
                attendancePage.getNumber(),
                attendancePage.getSize(),
                attendancePage.getTotalElements(),
                attendancePage.getTotalPages(),
                attendancePage.hasNext()
        );

        return PageResponse.of(items, pageMeta);
    }

    @Transactional
    public AttendanceResponse checkIn(Long userId, Long scheduleId, String code) {
        Attendance attendance = attendanceRepository.findByUser_UserIdAndSchedule_ScheduleId(userId, scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (attendance.getStatus() == AttendanceStatus.PRESENT) {
            return AttendanceResponse.from(attendance);
        }
        if (attendance.getStatus() != AttendanceStatus.NOT_CHECKED) {
            throw new BusinessException(ErrorCode.ATTENDANCE_ALREADY_FINALIZED);
        }
        if (!attendanceCodeService.matches(scheduleId, code)) {
            throw new BusinessException(ErrorCode.ATTENDANCE_CODE_INVALID);
        }

        attendance.checkIn(LocalDateTime.now(), CheckInSource.SELF_CODE);
        return AttendanceResponse.from(attendance);
    }
}
