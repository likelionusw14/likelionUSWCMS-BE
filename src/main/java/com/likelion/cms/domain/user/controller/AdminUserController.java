package com.likelion.cms.domain.user.controller;

import com.likelion.cms.domain.user.dto.request.ChangeAccountRoleRequest;
import com.likelion.cms.domain.user.dto.request.RejectAccountRequest;
import com.likelion.cms.domain.user.dto.request.UpdateAccountRequest;
import com.likelion.cms.domain.user.dto.response.AccountResponse;
import com.likelion.cms.domain.user.entity.AccountStatus;
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

// 관리자 전용 회원 관리 API. /api/users(일반 사용자용, 아직 미구현)와는
// 별도 컨트롤러로 분리 - 관리자 액션은 항상 AdminAccessGuard를 거치게 강제하기 위함.
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
public class AdminUserController {

    // 스펙 상 페이지 size는 최대 100까지만 허용 (한 번에 너무 많은 데이터 조회 방지).
    private static final int MAX_PAGE_SIZE = 100;

    private final UserService userService;
    private final AdminAccessGuard adminAccessGuard;

    // GET /api/admin/users?status=PENDING&page=0&size=20
    @GetMapping
    public PageResponse<AccountResponse> list(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam(required = false) AccountStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        adminAccessGuard.requireAdmin(principal);
        // 클라이언트가 size=1000 같은 값을 보내도 서버가 강제로 100까지만 잘라줌.
        Pageable boundedPageable = PageRequest.of(
                pageable.getPageNumber(), Math.min(pageable.getPageSize(), MAX_PAGE_SIZE), pageable.getSort());
        return userService.list(status, boundedPageable);
    }

    // PATCH /api/admin/users/{userId}/approve - 가입 승인 (바디 없음)
    @PatchMapping("/{userId}/approve")
    public AccountResponse approve(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable @Positive Long userId
    ) {
        Long actorUserId = adminAccessGuard.requireAdmin(principal);
        return userService.approve(userId, actorUserId);
    }

    // PATCH /api/admin/users/{userId}/reject - 가입 거절 (사유 필수)
    @PatchMapping("/{userId}/reject")
    public AccountResponse reject(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable @Positive Long userId,
            @Valid @RequestBody RejectAccountRequest request
    ) {
        Long actorUserId = adminAccessGuard.requireAdmin(principal);
        return userService.reject(userId, request, actorUserId);
    }

    // PATCH /api/admin/users/{userId}/role - 권한 변경 (낙관적 락 버전 필요)
    @PatchMapping("/{userId}/role")
    public AccountResponse changeRole(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable @Positive Long userId,
            @Valid @RequestBody ChangeAccountRoleRequest request
    ) {
        Long actorUserId = adminAccessGuard.requireAdmin(principal);
        return userService.changeRole(userId, request, actorUserId);
    }

    // PATCH /api/admin/users/{userId} - 회원 정보 부분 수정
    @PatchMapping("/{userId}")
    public AccountResponse update(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable @Positive Long userId,
            @Valid @RequestBody UpdateAccountRequest request
    ) {
        Long actorUserId = adminAccessGuard.requireAdmin(principal);
        return userService.update(userId, request, actorUserId);
    }

    // DELETE /api/admin/users/{userId} - 소프트 삭제, 성공 시 204 No Content
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
