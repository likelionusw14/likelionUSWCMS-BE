package com.likelion.cms.domain.auth.controller;

import com.likelion.cms.common.type.PartType;
import com.likelion.cms.domain.auth.dto.response.AccountResponse;
import com.likelion.cms.domain.auth.service.AccountOnboardingService;
import com.likelion.cms.domain.auth.service.CsrfCookieValidator;
import com.likelion.cms.domain.cohort.dto.response.CohortSummary;
import com.likelion.cms.domain.user.entity.AccountStatus;
import com.likelion.cms.domain.user.entity.SystemRole;
import com.likelion.cms.global.config.SecurityConfig;
import com.likelion.cms.global.exception.BusinessException;
import com.likelion.cms.global.exception.ErrorCode;
import com.likelion.cms.global.jwt.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AccountController.class)
@Import(SecurityConfig.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountOnboardingService accountOnboardingService;

    @MockitoBean
    private CsrfCookieValidator csrfCookieValidator;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void createsAccountWhenSessionAndCsrfAreValid() throws Exception {
        AccountResponse response = AccountResponse.of(1L, "홍길동", "컴퓨터공학과", "202012345",
                CohortSummary.of(1L, 14, "14기"), PartType.BACKEND, SystemRole.MEMBER, AccountStatus.PENDING,
                null, 0, LocalDateTime.now(), LocalDateTime.now());
        when(accountOnboardingService.createAccount(eq("session-1"), any())).thenReturn(response);

        mockMvc.perform(post("/api/accounts")
                        .cookie(new Cookie("onboarding_session", "session-1"), new Cookie("csrf_token", "csrf-1"))
                        .header("X-CSRF-Token", "csrf-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "홍길동",
                                  "department": "컴퓨터공학과",
                                  "studentId": "202012345",
                                  "cohortId": 1,
                                  "part": "BACKEND"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/accounts/1"))
                .andExpect(jsonPath("$.studentId").value("202012345"));
    }

    @Test
    void rejectsRequestWhenCsrfDoesNotMatch() throws Exception {
        doThrow(new BusinessException(ErrorCode.UNAUTHORIZED)).when(csrfCookieValidator).validate(any(), any());

        mockMvc.perform(post("/api/accounts")
                        .cookie(new Cookie("onboarding_session", "session-1"), new Cookie("csrf_token", "csrf-1"))
                        .header("X-CSRF-Token", "wrong")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "홍길동",
                                  "department": "컴퓨터공학과",
                                  "studentId": "202012345",
                                  "cohortId": 1,
                                  "part": "BACKEND"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("C004"));
    }

    @Test
    void rejectsInvalidRequestBody() throws Exception {
        mockMvc.perform(post("/api/accounts")
                        .cookie(new Cookie("onboarding_session", "session-1"), new Cookie("csrf_token", "csrf-1"))
                        .header("X-CSRF-Token", "csrf-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "department": "컴퓨터공학과",
                                  "studentId": "202012345",
                                  "cohortId": 1,
                                  "part": "BACKEND"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
        verify(accountOnboardingService, never()).createAccount(anyString(), any());
    }

    @Test
    void propagatesConflictFromService() throws Exception {
        when(accountOnboardingService.createAccount(eq("session-1"), any()))
                .thenThrow(new BusinessException(ErrorCode.CONFLICT, "이미 사용 중인 학번입니다."));

        mockMvc.perform(post("/api/accounts")
                        .cookie(new Cookie("onboarding_session", "session-1"), new Cookie("csrf_token", "csrf-1"))
                        .header("X-CSRF-Token", "csrf-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "홍길동",
                                  "department": "컴퓨터공학과",
                                  "studentId": "202012345",
                                  "cohortId": 1,
                                  "part": "BACKEND"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("C006"));
    }
}
