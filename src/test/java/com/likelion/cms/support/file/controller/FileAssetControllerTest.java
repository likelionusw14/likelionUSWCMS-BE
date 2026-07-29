package com.likelion.cms.support.file.controller;

import com.likelion.cms.domain.user.entity.SystemRole;
import com.likelion.cms.global.config.SecurityConfig;
import com.likelion.cms.global.exception.BusinessException;
import com.likelion.cms.global.exception.ErrorCode;
import com.likelion.cms.global.jwt.JwtTokenProvider;
import com.likelion.cms.global.security.AdminAccessGuard;
import com.likelion.cms.global.security.CurrentUserPrincipal;
import com.likelion.cms.support.file.dto.response.FileAssetResponse;
import com.likelion.cms.support.file.dto.response.FileUploadUrlResponse;
import com.likelion.cms.support.file.entity.FilePurpose;
import com.likelion.cms.support.file.service.FileAssetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FileAssetController.class)
@Import({AdminAccessGuard.class, SecurityConfig.class})
class FileAssetControllerTest {

    private static final String IDEMPOTENCY_KEY =
            "123e4567-e89b-12d3-a456-426614174000";
    private static final String OBJECT_KEY =
            "learning-resource/2026/07/25/123e4567-e89b-12d3-a456-426614174000.pdf";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FileAssetService fileAssetService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void adminCreatesUploadUrl() throws Exception {
        when(fileAssetService.createUploadUrl(any(), eq(7L))).thenReturn(
                FileUploadUrlResponse.of(
                        "https://example.s3.amazonaws.com/upload",
                        OBJECT_KEY,
                        Map.of("content-type", "application/pdf"),
                        OffsetDateTime.of(2026, 7, 25, 12, 10, 0, 0, ZoneOffset.UTC)
                )
        );

        mockMvc.perform(post("/api/admin/file-upload-urls")
                        .with(authentication(adminAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(uploadUrlRequestJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.objectKey").value(OBJECT_KEY))
                .andExpect(jsonPath("$.requiredHeaders.content-type")
                        .value("application/pdf"));
    }

    @Test
    void memberCannotCreateUploadUrl() throws Exception {
        mockMvc.perform(post("/api/admin/file-upload-urls")
                        .with(authentication(memberAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(uploadUrlRequestJson()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("C005"));

        verify(fileAssetService, never()).createUploadUrl(any(), any());
    }

    @Test
    void unauthenticatedRequestCannotCreateUploadUrl() throws Exception {
        mockMvc.perform(post("/api/admin/file-upload-urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(uploadUrlRequestJson()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("C004"));
    }

    @Test
    void malformedFileNameIsRejectedAtBoundary() throws Exception {
        mockMvc.perform(post("/api/admin/file-upload-urls")
                        .with(authentication(adminAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "purpose": "LEARNING_RESOURCE",
                                  "originalFileName": "../lecture.pdf",
                                  "mimeType": "application/pdf",
                                  "sizeBytes": 1024
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));

        verify(fileAssetService, never()).createUploadUrl(any(), any());
    }

    @Test
    void unsupportedMimeTypeReturns415() throws Exception {
        when(fileAssetService.createUploadUrl(any(), eq(7L)))
                .thenThrow(new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE));

        mockMvc.perform(post("/api/admin/file-upload-urls")
                        .with(authentication(adminAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "purpose": "LEARNING_RESOURCE",
                                  "originalFileName": "lecture.pptx",
                                  "mimeType": "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                                  "sizeBytes": 1024
                                }
                                """))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("F002"));
    }

    @Test
    void completesUploadAndReturnsLocation() throws Exception {
        when(fileAssetService.completeUpload(any(), eq(7L), any())).thenReturn(
                FileAssetResponse.of(
                        10L,
                        FilePurpose.LEARNING_RESOURCE,
                        "lecture.pdf",
                        "application/pdf",
                        1024L,
                        LocalDateTime.of(2026, 7, 25, 21, 0)
                )
        );

        mockMvc.perform(post("/api/admin/files")
                        .with(authentication(adminAuthentication()))
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fileAssetRequestJson()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/admin/files/10"))
                .andExpect(jsonPath("$.fileAssetId").value(10));
    }

    @Test
    void completeUploadRequiresValidIdempotencyKey() throws Exception {
        mockMvc.perform(post("/api/admin/files")
                        .with(authentication(adminAuthentication()))
                        .header("Idempotency-Key", "not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fileAssetRequestJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));

        verify(fileAssetService, never()).completeUpload(any(), any(), any());
    }

    private String uploadUrlRequestJson() {
        return """
                {
                  "purpose": "LEARNING_RESOURCE",
                  "originalFileName": "lecture.pdf",
                  "mimeType": "application/pdf",
                  "sizeBytes": 1024
                }
                """;
    }

    private String fileAssetRequestJson() {
        return """
                {
                  "purpose": "LEARNING_RESOURCE",
                  "objectKey": "%s",
                  "originalFileName": "lecture.pdf",
                  "mimeType": "application/pdf",
                  "sizeBytes": 1024
                }
                """.formatted(OBJECT_KEY);
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
