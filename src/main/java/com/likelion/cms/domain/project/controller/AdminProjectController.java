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

// 관리자 전용 프로젝트 관리 API. /api/projects(일반 조회용, 별도 이슈)와는
// 분리해서 관리자 액션은 항상 AdminAccessGuard를 거치게 함.
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/projects")
public class AdminProjectController {

    private final ProjectService projectService;
    private final AdminAccessGuard adminAccessGuard;

    // POST /api/admin/projects - 등록. Idempotency-Key 필수
    // (같은 키로 중복 요청이 와도 서버가 같은 작업을 두 번 처리하지 않도록 하는 용도 -
    //  단, 지금은 헤더 형식만 검증하고 실제 중복 방지 로직은 별도 구현 안 함)
    @PostMapping
    public ResponseEntity<ProjectResponse> create(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody CreateProjectRequest request
    ) {
        Long actorUserId = adminAccessGuard.requireAdmin(principal);
        ProjectResponse response = projectService.create(request, actorUserId);
        // 201 Created + Location 헤더로 새로 생긴 리소스의 조회 경로를 알려줌.
        return ResponseEntity.created(URI.create("/api/projects/" + response.projectId())).body(response);
    }

    // PATCH /api/admin/projects/{projectId} - 부분 수정
    @PatchMapping("/{projectId}")
    public ProjectResponse update(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable @Positive Long projectId,
            @Valid @RequestBody UpdateProjectRequest request
    ) {
        Long actorUserId = adminAccessGuard.requireAdmin(principal);
        return projectService.update(projectId, request, actorUserId);
    }

    // DELETE /api/admin/projects/{projectId} - 소프트 삭제
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
