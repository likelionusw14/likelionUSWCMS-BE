package com.likelion.cms.domain.auth.controller;

import com.likelion.cms.common.type.PartType;
import com.likelion.cms.domain.auth.dto.response.AccessTokenResponse;
import com.likelion.cms.domain.auth.dto.response.AccountResponse;
import com.likelion.cms.domain.auth.service.AuthCookieFactory;
import com.likelion.cms.domain.auth.service.AuthService;
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
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private AuthCookieFactory authCookieFactory;

    @MockitoBean
    private CsrfCookieValidator csrfCookieValidator;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void loginRedirectsToKakaoAuthorizeUrl() throws Exception {
        when(authService.startKakaoLogin(any())).thenReturn("https://kauth.kakao.com/oauth/authorize?client_id=x");

        mockMvc.perform(get("/api/auth/kakao/login"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://kauth.kakao.com/oauth/authorize?client_id=x"));
    }

    @Test
    void loginPassesRequestedOriginThrough() throws Exception {
        when(authService.startKakaoLogin("http://localhost:5173"))
                .thenReturn("https://kauth.kakao.com/oauth/authorize?client_id=x");

        mockMvc.perform(get("/api/auth/kakao/login").param("origin", "http://localhost:5173"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://kauth.kakao.com/oauth/authorize?client_id=x"));
    }

    @Test
    void callbackRedirectsAndSetsCookies() throws Exception {
        ResponseCookie cookie = ResponseCookie.from("refresh_session", "token").path("/api").build();
        when(authService.handleKakaoCallback("code-1", "state-1", null))
                .thenReturn(new AuthService.CallbackResult(URI.create("https://app.example.com"), List.of(cookie)));

        mockMvc.perform(get("/api/auth/kakao/callback").param("code", "code-1").param("state", "state-1"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://app.example.com"))
                .andExpect(header().exists("Set-Cookie"));
    }

    @Test
    void reissueRejectsRequestWhenCsrfDoesNotMatch() throws Exception {
        doThrow(new BusinessException(ErrorCode.UNAUTHORIZED)).when(csrfCookieValidator).validate(any(), any());

        mockMvc.perform(post("/api/auth/tokens")
                        .cookie(new Cookie("refresh_session", "token"), new Cookie("csrf_token", "abc"))
                        .header("X-CSRF-Token", "wrong"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("C004"));
    }

    @Test
    void reissueReturnsNewAccessTokenWhenCsrfMatches() throws Exception {
        AccountResponse account = AccountResponse.of(1L, "홍길동", "학과", "202012345",
                CohortSummary.of(1L, 14, "14기"), PartType.BACKEND, SystemRole.MEMBER, AccountStatus.ACTIVE,
                null, 0, LocalDateTime.now(), LocalDateTime.now());
        AccessTokenResponse body = AccessTokenResponse.of("jwt-value", "Bearer", 1800, account);
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_session", "new-token").path("/api").build();
        ResponseCookie csrfCookie = ResponseCookie.from("csrf_token", "new-csrf").path("/api").build();
        when(authService.reissueAccessToken("old-token"))
                .thenReturn(new AuthService.ReissueResult(body, refreshCookie, csrfCookie));

        mockMvc.perform(post("/api/auth/tokens")
                        .cookie(new Cookie("refresh_session", "old-token"), new Cookie("csrf_token", "csrf-1"))
                        .header("X-CSRF-Token", "csrf-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-value"));
    }

    @Test
    void logoutClearsSessionCookies() throws Exception {
        when(authCookieFactory.clearRefreshSessionCookie())
                .thenReturn(ResponseCookie.from("refresh_session", "").path("/api").maxAge(0).build());
        when(authCookieFactory.clearCsrfCookie())
                .thenReturn(ResponseCookie.from("csrf_token", "").path("/api").maxAge(0).build());

        mockMvc.perform(delete("/api/auth/session")
                        .cookie(new Cookie("refresh_session", "token"), new Cookie("csrf_token", "csrf-1"))
                        .header("X-CSRF-Token", "csrf-1"))
                .andExpect(status().isNoContent());
    }
}
