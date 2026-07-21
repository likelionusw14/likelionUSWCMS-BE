package com.likelion.cms.domain.project.controller;

import com.likelion.cms.domain.project.dto.request.CreateProjectRequest;
import com.likelion.cms.domain.project.dto.request.UpdateProjectRequest;
import com.likelion.cms.domain.project.dto.response.ProjectResponse;
import com.likelion.cms.domain.project.service.ProjectService;
import com.likelion.cms.global.security.AdminAccessGuard;
import com.likelion.cms.global.security.CurrentUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/admin/projects")
public class AdminProjectController {

    private final ProjectService projectService;
    private final AdminAccessGuard adminAccessGuard;

    @PostMapping
    public ResponseEntity<ProjectResponse> create(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody CreateProjectRequest request
    ) {
        Long actorUserId = adminAccessGuard.requireAdmin(principal);
        ProjectResponse response = projectService.create(request, actorUserId);
        return ResponseEntity.created(URI.create("/api/projects/" + response.projectId())).body(response);
    }

    @PatchMapping("/{projectId}")
    public ProjectResponse update(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable @Positive Long projectId,
            @Valid @RequestBody UpdateProjectRequest request
    ) {
        Long actorUserId = adminAccessGuard.requireAdmin(principal);
        return projectService.update(projectId, request, actorUserId);
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable @Positive Long projectId
    ) {
        Long actorUserId = adminAccessGuard.requireAdmin(principal);
        projectService.delete(projectId, actorUserId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
