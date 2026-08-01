package com.likelion.cms.domain.admin;

import com.likelion.cms.common.type.PartType;
import com.likelion.cms.domain.notice.controller.AdminNoticeController;
import com.likelion.cms.domain.notice.dto.request.UpdateNoticeRequest;
import com.likelion.cms.domain.notice.dto.response.NoticeResponse;
import com.likelion.cms.domain.notice.entity.NoticeTag;
import com.likelion.cms.domain.notice.service.NoticeService;
import com.likelion.cms.domain.resource.controller.AdminResourceController;
import com.likelion.cms.domain.resource.dto.response.LearningResourceResponse;
import com.likelion.cms.domain.resource.service.ResourceService;
import com.likelion.cms.domain.user.entity.SystemRole;
import com.likelion.cms.global.config.SecurityConfig;
import com.likelion.cms.global.jwt.JwtTokenProvider;
import com.likelion.cms.global.security.AdminAccessGuard;
import com.likelion.cms.global.security.CurrentUserPrincipal;
import com.likelion.cms.support.file.dto.response.FileAssetResponse;
import com.likelion.cms.support.file.entity.FilePurpose;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AdminResourceController.class, AdminNoticeController.class})
@Import({AdminAccessGuard.class, SecurityConfig.class})
class AdminContentControllerTest {

    private static final String IDEMPOTENCY_KEY = "123e4567-e89b-12d3-a456-426614174000";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResourceService resourceService;

    @MockitoBean
    private NoticeService noticeService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void createResourceReturnsCreatedResponseAndLocation() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 7, 20, 10, 0);
        FileAssetResponse file = FileAssetResponse.of(
                10L, FilePurpose.LEARNING_RESOURCE, "resource.pdf", "application/pdf", 1024L, now
        );
        LearningResourceResponse response = LearningResourceResponse.of(
                20L, "1주차 자료", 1, PartType.BACKEND, 7L, file, 0, now, now
        );
        when(resourceService.create(any(), eq(7L))).thenReturn(response);

        mockMvc.perform(post("/api/admin/resources")
                        .with(authentication(adminAuthentication()))
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "1주차 자료",
                                  "week": 1,
                                  "targetPart": "BACKEND",
                                  "fileAssetId": 10
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/resources/20"))
                .andExpect(jsonPath("$.resourceId").value(20));
    }

    @Test
    void createResourceRejectsMissingIdempotencyKey() throws Exception {
        mockMvc.perform(post("/api/admin/resources")
                        .with(authentication(adminAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "1주차 자료",
                                  "week": 1,
                                  "targetPart": "BACKEND",
                                  "fileAssetId": 10
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
        verify(resourceService, never()).create(any(), any());
    }

    @Test
    void createResourceRejectsMalformedIdempotencyKey() throws Exception {
        mockMvc.perform(post("/api/admin/resources")
                        .with(authentication(adminAuthentication()))
                        .header("Idempotency-Key", "not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "1주차 자료",
                                  "week": 1,
                                  "targetPart": "BACKEND",
                                  "fileAssetId": 10
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
        verify(resourceService, never()).create(any(), any());
    }

    @Test
    void createNoticeRejectsMemberRole() throws Exception {
        mockMvc.perform(post("/api/admin/notices")
                        .with(authentication(memberAuthentication()))
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "공지",
                                  "content": "내용",
                                  "tag": "OTHER"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("C005"));
        verify(noticeService, never()).create(any(), any());
    }

    @Test
    void updateNoticeRejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(patch("/api/admin/notices/30")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version": 1,
                                  "title": "수정 공지"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("C004"));
        verify(noticeService, never()).update(any(), any(), any());
    }

    @Test
    void updateNoticePreservesExplicitNullForImageRemoval() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 7, 20, 10, 0);
        NoticeResponse response = NoticeResponse.of(
                30L, "공지", "내용", NoticeTag.OTHER, false, null, null, 7L, now, 2, now, now
        );
        when(noticeService.update(eq(30L), any(), eq(7L))).thenReturn(response);

        mockMvc.perform(patch("/api/admin/notices/30")
                        .with(authentication(adminAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version": 1,
                                  "imageAssetId": null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noticeId").value(30));

        ArgumentCaptor<UpdateNoticeRequest> requestCaptor = ArgumentCaptor.forClass(UpdateNoticeRequest.class);
        verify(noticeService).update(eq(30L), requestCaptor.capture(), eq(7L));
        assertThat(requestCaptor.getValue().isImageAssetIdProvided()).isTrue();
        assertThat(requestCaptor.getValue().getImageAssetId()).isNull();
    }

    @Test
    void createNoticeRejectsUnsafeExternalUrl() throws Exception {
        mockMvc.perform(post("/api/admin/notices")
                        .with(authentication(adminAuthentication()))
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "공지",
                                  "content": "내용",
                                  "tag": "OTHER",
                                  "externalUrl": "javascript:alert(1)"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
        verify(noticeService, never()).create(any(), any());
    }

    @Test
    void updateNoticeDoesNotAcceptInternalPresenceFlagAsChange() throws Exception {
        mockMvc.perform(patch("/api/admin/notices/30")
                        .with(authentication(adminAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version": 1,
                                  "imageAssetIdProvided": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
        verify(noticeService, never()).update(any(), any(), any());
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
