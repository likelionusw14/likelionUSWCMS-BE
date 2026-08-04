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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * #86 재설계(scheduleId -> attendanceDate)로 인해 임시로 컴파일만 되게 손댄 상태입니다.
 * (담당: 정현윤이 빌드를 살리기 위해 최소 수정, 실제 로직/시그니처 재설계는 신준호님 확인 필요)
 *
 * TODO(신준호): scheduleId 개념이 사라졌으므로, checkIn의 scheduleId 파라미터를
 * 제거하고 attendanceDate(보통 LocalDate.now())를 쓰는 방향으로 시그니처/경로를
 * 재검토해주세요. 지금은 컴파일만 되도록 scheduleId 값은 무시하고 LocalDate.now()로
 * 대체해뒀습니다.
 */
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
        // TODO(신준호): scheduleId는 더 이상 유효한 식별자가 아님. attendanceDate 기준으로 재설계 필요.
        LocalDate attendanceDate = LocalDate.now();

        Attendance attendance = attendanceRepository.findByUser_UserIdAndAttendanceDate(userId, attendanceDate)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (attendance.getStatus() == AttendanceStatus.PRESENT) {
            return AttendanceResponse.from(attendance);
        }
        if (attendance.getStatus() != AttendanceStatus.NOT_CHECKED) {
            throw new BusinessException(ErrorCode.ATTENDANCE_ALREADY_FINALIZED);
        }
        if (!attendanceCodeService.matches(attendanceDate, code)) {
            throw new BusinessException(ErrorCode.ATTENDANCE_CODE_INVALID);
        }

        attendance.checkIn(LocalDateTime.now(), CheckInSource.SELF_CODE);
        return AttendanceResponse.from(attendance);
    }
}