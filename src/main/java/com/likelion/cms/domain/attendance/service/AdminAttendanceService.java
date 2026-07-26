package com.likelion.cms.domain.attendance.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.likelion.cms.common.type.PartType;
import com.likelion.cms.domain.attendance.dto.request.UpdateAttendanceRequest;
import com.likelion.cms.domain.attendance.dto.response.AttendanceCodeResponse;
import com.likelion.cms.domain.attendance.dto.response.AttendanceResponse;
import com.likelion.cms.domain.attendance.entity.Attendance;
import com.likelion.cms.domain.attendance.entity.AttendanceStatus;
import com.likelion.cms.domain.attendance.repository.AttendanceRepository;
import com.likelion.cms.domain.schedule.entity.Schedule;
import com.likelion.cms.domain.schedule.repository.ScheduleRepository;
import com.likelion.cms.domain.user.entity.AccountStatus;
import com.likelion.cms.domain.user.entity.AppUser;
import com.likelion.cms.domain.user.entity.SystemRole;
import com.likelion.cms.domain.user.repository.AppUserRepository;
import com.likelion.cms.global.exception.BusinessException;
import com.likelion.cms.global.exception.ErrorCode;
import com.likelion.cms.global.response.PageMeta;
import com.likelion.cms.global.response.PageResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminAttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final ScheduleRepository scheduleRepository;
    private final AppUserRepository appUserRepository;
    private final AttendanceCodeService attendanceCodeService;

    @Transactional(readOnly = true)
    public PageResponse<AttendanceResponse> listAttendances(
            Long scheduleId, PartType part, Long userId, AttendanceStatus status, Pageable pageable) {

        scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        Page<Attendance> result = attendanceRepository.searchForAdmin(scheduleId, part, userId, status, pageable);

        return PageResponse.of(
                result.getContent().stream()
                        .map(AttendanceResponse::from)
                        .toList(),
                PageMeta.of(
                        result.getNumber(),
                        result.getSize(),
                        result.getTotalElements(),
                        result.getTotalPages(),
                        result.hasNext()
                )
        );
    }

    public AttendanceResponse updateAttendance(Long attendanceId, UpdateAttendanceRequest request, Long actorUserId) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!attendance.getVersion().equals(request.getVersion())) {
            throw new BusinessException(ErrorCode.OPTIMISTIC_LOCK_CONFLICT);
        }

        AppUser admin = appUserRepository.getReferenceById(actorUserId);

        attendance.updateByAdmin(
                request.getStatus().toEntityStatus(),
                request.getMemo(),
                request.isMemoProvided(),
                admin,
                LocalDateTime.now()
        );

        return AttendanceResponse.from(attendance);
    }

    public AttendanceCodeResponse createOrReissueCode(Long scheduleId, Long actorUserId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        ensureAttendanceRowsExist(schedule);

        AttendanceCodeCacheValue value = attendanceCodeService.issue(scheduleId);
        return toResponse(scheduleId, value);
    }

    @Transactional(readOnly = true)
    public AttendanceCodeResponse getCurrentCode(Long scheduleId) {
        scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        return attendanceCodeService.getCurrent(scheduleId)
                .map(value -> toResponse(scheduleId, value))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private void ensureAttendanceRowsExist(Schedule schedule) {

        List<AppUser> targets = appUserRepository.findAllByCohort_CohortIdAndSystemRoleAndAccountStatus(
                schedule.getCohort().getCohortId(), SystemRole.MEMBER, AccountStatus.ACTIVE);

        List<Long> existingUserIds = attendanceRepository.findUserIdsByScheduleId(schedule.getScheduleId());

        List<Attendance> toCreate = targets.stream()
                .filter(user -> !existingUserIds.contains(user.getUserId()))
                .map(user -> Attendance.builder()
                        .user(user)
                        .schedule(schedule)
                        .build())
                .toList();

        if (!toCreate.isEmpty()) {
            attendanceRepository.saveAll(toCreate);
        }
    }

    private AttendanceCodeResponse toResponse(Long scheduleId, AttendanceCodeCacheValue value) {
        LocalDateTime expiresAt = value.startedAt().plusSeconds(300);
        return AttendanceCodeResponse.of(scheduleId, value.code(), value.startedAt(), expiresAt);
    }
}