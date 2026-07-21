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

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserService userService;
    private final AdminAccessGuard adminAccessGuard;

    @GetMapping
    public PageResponse<AccountResponse> list(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam(required = false) AccountStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        adminAccessGuard.requireAdmin(principal);
        Pageable boundedPageable = PageRequest.of(
                pageable.getPageNumber(), Math.min(pageable.getPageSize(), MAX_PAGE_SIZE), pageable.getSort());
        return userService.list(status, boundedPageable);
    }

    @PatchMapping("/{userId}/approve")
    public AccountResponse approve(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable @Positive Long userId
    ) {
        Long actorUserId = adminAccessGuard.requireAdmin(principal);
        return userService.approve(userId, actorUserId);
    }

    @PatchMapping("/{userId}/reject")
    public AccountResponse reject(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable @Positive Long userId,
            @Valid @RequestBody RejectAccountRequest request
    ) {
        Long actorUserId = adminAccessGuard.requireAdmin(principal);
        return userService.reject(userId, request, actorUserId);
    }

    @PatchMapping("/{userId}/role")
    public AccountResponse changeRole(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable @Positive Long userId,
            @Valid @RequestBody ChangeAccountRoleRequest request
    ) {
        Long actorUserId = adminAccessGuard.requireAdmin(principal);
        return userService.changeRole(userId, request, actorUserId);
    }

    @PatchMapping("/{userId}")
    public AccountResponse update(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable @Positive Long userId,
            @Valid @RequestBody UpdateAccountRequest request
    ) {
        Long actorUserId = adminAccessGuard.requireAdmin(principal);
        return userService.update(userId, request, actorUserId);
    }

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
