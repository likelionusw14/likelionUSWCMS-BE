package com.likelion.cms.domain.user.controller;

import com.likelion.cms.domain.cohort.dto.response.CohortSummary;
import com.likelion.cms.domain.user.dto.response.AccountResponse;
import com.likelion.cms.domain.user.entity.AccountStatus;
import com.likelion.cms.domain.user.entity.SystemRole;
import com.likelion.cms.domain.user.service.UserService;
import com.likelion.cms.global.config.SecurityConfig;
import com.likelion.cms.global.response.PageMeta;
import com.likelion.cms.global.response.PageResponse;
import com.likelion.cms.global.security.AdminAccessGuard;
import com.likelion.cms.global.security.CurrentUserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminUserController.class)
@Import({AdminAccessGuard.class, SecurityConfig.class})
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void approveReturnsUpdatedAccount() throws Exception {
        when(userService.approve(2L, 7L)).thenReturn(accountResponse());

        mockMvc.perform(patch("/api/admin/users/2/approve")
                        .with(authentication(adminAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(2))
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"));
    }

    @Test
    void approveRejectsMemberRole() throws Exception {
        mockMvc.perform(patch("/api/admin/users/2/approve")
                        .with(authentication(memberAuthentication())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("C005"));
        verify(userService, never()).approve(any(), any());
    }

    @Test
    void rejectRejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(patch("/api/admin/users/2/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "rejectionReason": "서류 미비" }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("C004"));
        verify(userService, never()).reject(any(), any(), any());
    }

    @Test
    void rejectRejectsBlankReason() throws Exception {
        mockMvc.perform(patch("/api/admin/users/2/reject")
                        .with(authentication(adminAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "rejectionReason": "" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
        verify(userService, never()).reject(any(), any(), any());
    }

    @Test
    void changeRoleRejectsMissingVersion() throws Exception {
        mockMvc.perform(patch("/api/admin/users/2/role")
                        .with(authentication(adminAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "systemRole": "ADMIN" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
        verify(userService, never()).changeRole(any(), any(), any());
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/admin/users/2")
                        .with(authentication(adminAuthentication())))
                .andExpect(status().isNoContent());
    }

    @Test
    void listReturnsPagedAccounts() throws Exception {
        PageResponse<AccountResponse> page = PageResponse.of(
                List.of(accountResponse()), PageMeta.of(0, 20, 1, 1, false));
        when(userService.list(eq(AccountStatus.PENDING), any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/users?status=PENDING")
                        .with(authentication(adminAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].userId").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    private AccountResponse accountResponse() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 21, 10, 0);
        return new AccountResponse(
                2L, "홍길동", "컴퓨터공학과", "2021000000",
                CohortSummary.of(1L, 5, "5기"), null, SystemRole.MEMBER, AccountStatus.ACTIVE,
                now, 7L, null, null, null, 0, now, now
        );
    }

    private UsernamePasswordAuthenticationToken adminAuthentication() {
        CurrentUserPrincipal principal = new CurrentUserPrincipal(7L, SystemRole.ADMIN);
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    private UsernamePasswordAuthenticationToken memberAuthentication() {
        CurrentUserPrincipal principal = new CurrentUserPrincipal(8L, SystemRole.MEMBER);
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }
}
