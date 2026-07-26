package com.likelion.cms.support.file.controller;

import com.likelion.cms.global.security.AdminAccessGuard;
import com.likelion.cms.global.security.CurrentUserPrincipal;
import com.likelion.cms.support.file.dto.request.FileAssetRequest;
import com.likelion.cms.support.file.dto.request.FileUploadUrlRequest;
import com.likelion.cms.support.file.dto.response.FileAssetResponse;
import com.likelion.cms.support.file.dto.response.FileUploadUrlResponse;
import com.likelion.cms.support.file.service.FileAssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class FileAssetController {

    private final FileAssetService fileAssetService;
    private final AdminAccessGuard adminAccessGuard;

    @Operation(summary = "파일 업로드 URL 발급")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Presigned PUT URL 발급"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
            @ApiResponse(responseCode = "413", description = "파일 크기 제한 초과"),
            @ApiResponse(responseCode = "415", description = "지원하지 않는 파일 형식")
    })
    @PostMapping("/file-upload-urls")
    public ResponseEntity<FileUploadUrlResponse> createUploadUrl(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody FileUploadUrlRequest request
    ) {
        Long actorUserId = adminAccessGuard.requireAdmin(principal);
        return ResponseEntity.status(201)
                .body(fileAssetService.createUploadUrl(request, actorUserId));
    }

    @Operation(summary = "업로드 완료 검증 및 파일 자산 등록")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "파일 자산 등록 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
            @ApiResponse(responseCode = "404", description = "발급 기록 또는 S3 객체 없음"),
            @ApiResponse(responseCode = "409", description = "멱등 키 또는 파일 자산 충돌"),
            @ApiResponse(responseCode = "413", description = "파일 크기 제한 초과"),
            @ApiResponse(responseCode = "415", description = "지원하지 않는 파일 형식"),
            @ApiResponse(responseCode = "422", description = "업로드 메타데이터 불일치"),
            @ApiResponse(responseCode = "502", description = "S3 연동 실패")
    })
    @PostMapping("/files")
    public ResponseEntity<FileAssetResponse> completeUpload(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody FileAssetRequest request
    ) {
        Long actorUserId = adminAccessGuard.requireAdmin(principal);
        FileAssetResponse response = fileAssetService.completeUpload(
                request,
                actorUserId,
                idempotencyKey
        );
        return ResponseEntity.created(
                URI.create("/api/admin/files/" + response.getFileAssetId())
        ).body(response);
    }
}
