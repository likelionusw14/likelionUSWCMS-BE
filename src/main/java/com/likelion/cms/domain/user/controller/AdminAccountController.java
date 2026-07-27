package com.likelion.cms.domain.user.controller;

import com.likelion.cms.common.type.PartType;
import com.likelion.cms.domain.user.dto.request.ApproveAccountRequest;
import com.likelion.cms.domain.user.dto.request.RejectAccountRequest;
import com.likelion.cms.domain.user.dto.request.UpdateAccountRequest;
import com.likelion.cms.domain.user.dto.request.UpdateRoleRequest;
import com.likelion.cms.domain.user.dto.response.AccountResponse;
import com.likelion.cms.domain.user.entity.AccountStatus;
import com.likelion.cms.domain.user.entity.SystemRole;
import com.likelion.cms.domain.user.service.UserService;
import com.likelion.cms.global.response.PageResponse;
import com.likelion.cms.global.security.AdminAccessGuard;
import com.likelion.cms.global.security.CurrentUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 관리자 전용 회원(계정) 관리 API. 라우팅/엔드포인트 구성은 팀에서 작성한
// admin/accounts API 목록을 그대로 따름 (가입 승인/거절 분리 등).
// 도메인 패키지 자체는 domain/user 그대로 - AppUser 엔티티 이름과 무관하게
// API 계약(URL/응답 필드명)만 팀 문서에 맞춤.
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/accounts")
public class AdminAccountController {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserService userService;
    private final AdminAccessGuard adminAccessGuard;

    // GET /api/admin/accounts?status=&role=&cohortId=&part=&keyword=&page=&size=
    @GetMapping
    public PageResponse<AccountResponse> list(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(required = false) SystemRole role,
            @RequestParam(required = false) Long cohortId,
            @RequestParam(required = false) PartType part,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        adminAccessGuard.requireAdmin(principal);
        // 클라이언트가 size=1000 같은 값을 보내도 서버가 강제로 100까지만 잘라줌.
        Pageable boundedPageable = PageRequest.of(
                pageable.getPageNumber(), Math.min(pageable.getPageSize(), MAX_PAGE_SIZE));
        return userService.list(status, role, cohortId, part, keyword, boundedPageable);
    }

    // PATCH /api/admin/accounts/{userId}/approval - 가입 승인
    @PatchMapping("/{userId}/approval")
    public AccountResponse approve(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable @Positive Long userId,
            @Valid @RequestBody ApproveAccountRequest request
    ) {
        Long actorUserId = adminAccessGuard.requireAdmin(principal);
        return userService.approve(userId, request, actorUserId);
    }

    // PATCH /api/admin/accounts/{userId}/rejection - 가입 거절 (사유 필수)
    @PatchMapping("/{userId}/rejection")
    public AccountResponse reject(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable @Positive Long userId,
            @Valid @RequestBody RejectAccountRequest request
    ) {
        Long actorUserId = adminAccessGuard.requireAdmin(principal);
        return userService.reject(userId, request, actorUserId);
    }

    // PATCH /api/admin/accounts/{userId}/role - 권한 변경 (낙관적 락 버전 필요)
    @PatchMapping("/{userId}/role")
    public AccountResponse changeRole(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable @Positive Long userId,
            @Valid @RequestBody UpdateRoleRequest request
    ) {
        Long actorUserId = adminAccessGuard.requireAdmin(principal);
        return userService.changeRole(userId, request, actorUserId);
    }

    // PATCH /api/admin/accounts/{userId} - 회원 정보 부분 수정
    @PatchMapping("/{userId}")
    public AccountResponse update(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable @Positive Long userId,
            @Valid @RequestBody UpdateAccountRequest request
    ) {
        Long actorUserId = adminAccessGuard.requireAdmin(principal);
        return userService.update(userId, request, actorUserId);
    }

    // DELETE /api/admin/accounts/{userId} - 소프트 삭제, 성공 시 204 No Content
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable @Positive Long userId
    ) {
        Long actorUserId = adminAccessGuard.requireAdmin(principal);
        userService.delete(userId, actorUserId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
