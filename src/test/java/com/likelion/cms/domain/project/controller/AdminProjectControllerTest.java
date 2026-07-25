package com.likelion.cms.domain.project.controller;

import com.likelion.cms.common.type.ProjectType;
import com.likelion.cms.domain.cohort.dto.response.CohortSummary;
import com.likelion.cms.domain.project.dto.response.AdminProjectResponse;
import com.likelion.cms.domain.project.service.ProjectService;
import com.likelion.cms.domain.user.entity.SystemRole;
import com.likelion.cms.global.config.SecurityConfig;
import com.likelion.cms.global.jwt.JwtTokenProvider;
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
import java.time.YearMonth;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminProjectController.class)
@Import({AdminAccessGuard.class, SecurityConfig.class})
class AdminProjectControllerTest {

    private static final String IDEMPOTENCY_KEY = "123e4567-e89b-12d3-a456-426614174000";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectService projectService;

    // #38(카카오 로그인) 머지 이후 SecurityConfig가 JwtTokenProvider를 의존하게 돼서
    // @Import(SecurityConfig.class)가 있는 이 테스트도 빈을 못 찾아 컨텍스트 로딩이 깨짐.
    // 실제 토큰 검증 로직은 안 쓰니 mock으로만 채워둠.
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void createReturnsCreatedResponseAndLocation() throws Exception {
        when(projectService.create(any(), eq(7L))).thenReturn(projectResponse());

        mockMvc.perform(post("/api/admin/projects")
                        .with(authentication(adminAuthentication()))
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "새 프로젝트",
                                  "description": "설명",
                                  "projectType": "HACKATHON",
                                  "cohortId": 5,
                                  "startedMonth": "2026-01-01",
                                  "endedMonth": "2026-06-01"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/projects/20"))
                .andExpect(jsonPath("$.projectId").value(20));
    }

    @Test
    void createRejectsMissingIdempotencyKey() throws Exception {
        mockMvc.perform(post("/api/admin/projects")
                        .with(authentication(adminAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "새 프로젝트",
                                  "description": "설명",
                                  "projectType": "HACKATHON",
                                  "cohortId": 5,
                                  "startedMonth": "2026-01-01",
                                  "endedMonth": "2026-06-01"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
        verify(projectService, never()).create(any(), any());
    }

    @Test
    void createRejectsMemberRole() throws Exception {
        mockMvc.perform(post("/api/admin/projects")
                        .with(authentication(memberAuthentication()))
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "새 프로젝트",
                                  "description": "설명",
                                  "projectType": "HACKATHON",
                                  "cohortId": 5,
                                  "startedMonth": "2026-01-01",
                                  "endedMonth": "2026-06-01"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("C005"));
        verify(projectService, never()).create(any(), any());
    }

    @Test
    void createRejectsInvalidDeployUrl() throws Exception {
        mockMvc.perform(post("/api/admin/projects")
                        .with(authentication(adminAuthentication()))
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "새 프로젝트",
                                  "description": "설명",
                                  "projectType": "HACKATHON",
                                  "cohortId": 5,
                                  "startedMonth": "2026-01-01",
                                  "endedMonth": "2026-06-01",
                                  "deployUrl": "javascript:alert(1)"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
        verify(projectService, never()).create(any(), any());
    }

    @Test
    void updateRejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(patch("/api/admin/projects/30")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version": 1,
                                  "title": "수정된 제목"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("C004"));
        verify(projectService, never()).update(any(), any(), any());
    }

    @Test
    void updatePreservesExplicitNullForThumbnailRemoval() throws Exception {
        when(projectService.update(eq(30L), any(), eq(7L))).thenReturn(projectResponse());

        mockMvc.perform(patch("/api/admin/projects/30")
                        .with(authentication(adminAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version": 1,
                                  "thumbnailAssetId": null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(20));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/admin/projects/30")
                        .with(authentication(adminAuthentication())))
                .andExpect(status().isNoContent());
    }

    private AdminProjectResponse projectResponse() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 21, 10, 0);
        return AdminProjectResponse.of(
                20L, "새 프로젝트", "설명", ProjectType.HACKATHON, null, "https://example.com", "https://github.com/example/repo",
                CohortSummary.of(5L, 5, "5기"), YearMonth.of(2026, 1), YearMonth.of(2026, 6),
                7L, 0, now, now
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
