package com.likelion.cms.domain.attendance.controller;

import java.net.URI;
import java.time.LocalDate;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.likelion.cms.common.type.PartType;
import com.likelion.cms.domain.attendance.dto.request.UpdateAttendanceRequest;
import com.likelion.cms.domain.attendance.dto.response.AttendanceCodeResponse;
import com.likelion.cms.domain.attendance.dto.response.AttendanceResponse;
import com.likelion.cms.domain.attendance.entity.AttendanceStatus;
import com.likelion.cms.domain.attendance.service.AdminAttendanceService;
import com.likelion.cms.global.response.PageResponse;
import com.likelion.cms.global.security.AdminAccessGuard;
import com.likelion.cms.global.security.CurrentUserPrincipal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminAttendanceController {

    private final AdminAttendanceService adminAttendanceService;
    private final AdminAccessGuard adminAccessGuard;

    @GetMapping("/attendances")
    public PageResponse<AttendanceResponse> listAttendances(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) PartType part,
            @RequestParam(required = false) @Positive Long userId,
            @RequestParam(required = false) AttendanceStatus status,
            @PageableDefault(size = 20, sort = {"user.part", "user.name"}) Pageable pageable
    ) {
        adminAccessGuard.requireAdmin(principal);
        return adminAttendanceService.listAttendances(date, part, userId, status, pageable);
    }

    @PatchMapping("/attendances/{attendanceId}")
    public AttendanceResponse updateAttendance(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable @Positive Long attendanceId,
            @Valid @RequestBody UpdateAttendanceRequest request
    ) {
        Long actorUserId = adminAccessGuard.requireAdmin(principal);
        return adminAttendanceService.updateAttendance(attendanceId, request, actorUserId);
    }

    @PostMapping("/attendance-code/{date}")
    public ResponseEntity<AttendanceCodeResponse> createOrReissueCode(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long actorUserId = adminAccessGuard.requireAdmin(principal);
        AttendanceCodeResponse response = adminAttendanceService.createOrReissueCode(date, actorUserId);
        return ResponseEntity
                .created(URI.create("/api/admin/attendance-code/" + date))
                .body(response);
    }

    @GetMapping("/attendance-code/{date}")
    public AttendanceCodeResponse getCurrentCode(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        adminAccessGuard.requireAdmin(principal);
        return adminAttendanceService.getCurrentCode(date);
    }
}