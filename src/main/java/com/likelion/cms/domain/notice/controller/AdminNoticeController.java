package com.likelion.cms.domain.notice.controller;

import com.likelion.cms.domain.notice.dto.request.CreateNoticeRequest;
import com.likelion.cms.domain.notice.dto.request.UpdateNoticeRequest;
import com.likelion.cms.domain.notice.dto.response.NoticeResponse;
import com.likelion.cms.domain.notice.service.NoticeService;
import com.likelion.cms.global.security.AdminAccessGuard;
import com.likelion.cms.global.security.CurrentUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/notices")
public class AdminNoticeController {

    private final NoticeService noticeService;
    private final AdminAccessGuard adminAccessGuard;

    @PostMapping
    public ResponseEntity<NoticeResponse> create(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody CreateNoticeRequest request
    ) {
        Long actorUserId = adminAccessGuard.requireAdmin(principal);
        NoticeResponse response = noticeService.create(request, actorUserId);
        return ResponseEntity.created(URI.create("/api/notices/" + response.getNoticeId())).body(response);
    }

    @PatchMapping("/{noticeId}")
    public NoticeResponse update(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable @Positive Long noticeId,
            @Valid @RequestBody UpdateNoticeRequest request
    ) {
        Long actorUserId = adminAccessGuard.requireAdmin(principal);
        return noticeService.update(noticeId, request, actorUserId);
    }
}
