package com.likelion.cms.domain.certificate.controller;

import com.likelion.cms.domain.certificate.dto.response.CertificatePreviewResponse;
import com.likelion.cms.domain.certificate.dto.response.CertificateResponse;
import com.likelion.cms.domain.certificate.dto.response.DownloadUrlResponse;
import com.likelion.cms.domain.certificate.service.CertificateService;
import com.likelion.cms.global.security.AccountStatusGuard;
import com.likelion.cms.global.security.CurrentUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/certificates")
public class CertificateController {

    private final CertificateService certificateService;
    private final AccountStatusGuard accountStatusGuard;

    @GetMapping("/preview")
    public CertificatePreviewResponse previewMyCertificateData(
            @AuthenticationPrincipal CurrentUserPrincipal principal
    ) {
        Long userId = accountStatusGuard.requireActive(principal);
        return certificateService.previewMyCertificateData(userId);
    }

    @GetMapping("/{certificateId}/download-url")
    public DownloadUrlResponse getDownloadUrl(
            @PathVariable Long certificateId,
            @AuthenticationPrincipal CurrentUserPrincipal principal
    ) {
        Long userId = accountStatusGuard.requireActive(principal);
        return certificateService.getDownloadUrl(certificateId, userId);
    }

    @PostMapping
    public ResponseEntity<CertificateResponse> issueCertificate(
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @AuthenticationPrincipal CurrentUserPrincipal principal
    ) {
        Long userId = accountStatusGuard.requireActive(principal);
        CertificateResponse response = certificateService.issueCertificate(userId, idempotencyKey);
        return ResponseEntity
                .created(URI.create("/api/certificates/" + response.getCertificateId()))
                .body(response);
    }
}