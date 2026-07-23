package com.likelion.cms.domain.resource.controller;

import com.likelion.cms.domain.resource.dto.request.CreateLearningResourceRequest;
import com.likelion.cms.domain.resource.dto.request.UpdateLearningResourceRequest;
import com.likelion.cms.domain.resource.dto.response.LearningResourceResponse;
import com.likelion.cms.domain.resource.service.ResourceService;
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
@RequestMapping("/api/admin/resources")
public class AdminResourceController {

    private final ResourceService resourceService;
    private final AdminAccessGuard adminAccessGuard;

    @PostMapping
    public ResponseEntity<LearningResourceResponse> create(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody CreateLearningResourceRequest request
    ) {
        Long actorUserId = adminAccessGuard.requireAdmin(principal);
        LearningResourceResponse response = resourceService.create(request, actorUserId);
        return ResponseEntity.created(URI.create("/api/resources/" + response.getResourceId())).body(response);
    }

    @PatchMapping("/{resourceId}")
    public LearningResourceResponse update(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable @Positive Long resourceId,
            @Valid @RequestBody UpdateLearningResourceRequest request
    ) {
        Long actorUserId = adminAccessGuard.requireAdmin(principal);
        return resourceService.update(resourceId, request, actorUserId);
    }
}
