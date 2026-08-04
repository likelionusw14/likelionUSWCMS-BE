package com.likelion.cms.domain.attendance.service;

import java.time.LocalDate;
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
    private final AppUserRepository appUserRepository;
    private final AttendanceCodeService attendanceCodeService;

    @Transactional(readOnly = true)
    public PageResponse<AttendanceResponse> listAttendances(
            LocalDate attendanceDate, PartType part, Long userId, AttendanceStatus status, Pageable pageable) {

        Page<Attendance> result =
                attendanceRepository.searchForAdmin(attendanceDate, part, userId, status, pageable);

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

    public AttendanceCodeResponse createOrReissueCode(LocalDate attendanceDate, Long actorUserId) {
        ensureAttendanceRowsExist(attendanceDate);

        AttendanceCodeCacheValue value = attendanceCodeService.issue(attendanceDate);
        return toResponse(attendanceDate, value);
    }

    @Transactional(readOnly = true)
    public AttendanceCodeResponse getCurrentCode(LocalDate attendanceDate) {
        return attendanceCodeService.getCurrent(attendanceDate)
                .map(value -> toResponse(attendanceDate, value))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private void ensureAttendanceRowsExist(LocalDate attendanceDate) {
        List<AppUser> targets = appUserRepository.findAllBySystemRoleAndAccountStatus(
                SystemRole.MEMBER, AccountStatus.ACTIVE);

        List<Long> existingUserIds = attendanceRepository.findUserIdsByAttendanceDate(attendanceDate);

        List<Attendance> toCreate = targets.stream()
                .filter(user -> !existingUserIds.contains(user.getUserId()))
                .map(user -> Attendance.builder()
                        .user(user)
                        .attendanceDate(attendanceDate)
                        .build())
                .toList();

        if (!toCreate.isEmpty()) {
            attendanceRepository.saveAll(toCreate);
        }
    }

    private AttendanceCodeResponse toResponse(LocalDate attendanceDate, AttendanceCodeCacheValue value) {
        LocalDateTime expiresAt = value.startedAt().plusSeconds(300);
        return AttendanceCodeResponse.of(attendanceDate, value.code(), value.startedAt(), expiresAt);
    }
}