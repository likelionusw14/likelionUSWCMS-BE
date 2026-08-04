package com.likelion.cms.domain.attendance.controller;

import com.likelion.cms.domain.attendance.dto.request.AttendanceCheckInRequest;
import com.likelion.cms.domain.attendance.dto.response.AttendanceResponse;
import com.likelion.cms.domain.attendance.service.AttendanceService;
import com.likelion.cms.global.response.PageResponse;
import com.likelion.cms.global.security.AccountStatusGuard;
import com.likelion.cms.global.security.CurrentUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 경로가 /api/attendances(본인 목록)와 /api/attendance-check-ins(코드 인증)로
 * 나뉘어 있어 class 레벨 @RequestMapping 대신 메서드별 전체 경로를 사용한다.
 */
@Validated
@RestController
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final AccountStatusGuard accountStatusGuard;

    @GetMapping("/api/attendances")
    public PageResponse<AttendanceResponse> listMine(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Long userId = accountStatusGuard.requireActive(principal);
        return attendanceService.listMine(userId, page, size);
    }

    @PostMapping("/api/attendance-check-ins")
    public AttendanceResponse checkIn(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody AttendanceCheckInRequest request
    ) {
        Long userId = accountStatusGuard.requireActive(principal);
        return attendanceService.checkIn(userId, request.code());
    }
}
