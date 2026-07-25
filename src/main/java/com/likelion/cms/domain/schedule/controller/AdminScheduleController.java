package com.likelion.cms.domain.schedule.controller;

import com.likelion.cms.domain.schedule.dto.request.CreateScheduleRequest;
import com.likelion.cms.domain.schedule.dto.request.UpdateScheduleRequest;
import com.likelion.cms.domain.schedule.dto.response.ScheduleResponse;
import com.likelion.cms.domain.schedule.service.ScheduleService;
import com.likelion.cms.global.security.AdminAccessGuard;
import com.likelion.cms.global.security.CurrentUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/schedules")
public class AdminScheduleController {

    private final ScheduleService scheduleService;
    private final AdminAccessGuard adminAccessGuard;

    @PostMapping
    public ResponseEntity<ScheduleResponse> create(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody CreateScheduleRequest request
    ) {
        Long actorUserId = adminAccessGuard.requireAdmin(principal);
        ScheduleResponse response = scheduleService.create(request, actorUserId);
        return ResponseEntity.created(URI.create("/api/schedules/" + response.getScheduleId())).body(response);
    }

    @PatchMapping("/{scheduleId}")
    public ScheduleResponse update(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable @Positive Long scheduleId,
            @Valid @RequestBody UpdateScheduleRequest request
    ) {
        Long actorUserId = adminAccessGuard.requireAdmin(principal);
        return scheduleService.update(scheduleId, request, actorUserId);
    }

    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable @Positive Long scheduleId
    ) {
        Long actorUserId = adminAccessGuard.requireAdmin(principal);
        scheduleService.delete(scheduleId, actorUserId);
        return ResponseEntity.noContent().build();
    }
}