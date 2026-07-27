package com.likelion.cms.domain.certificate.controller;

import com.likelion.cms.domain.certificate.dto.response.CertificatePreviewResponse;
import com.likelion.cms.domain.certificate.dto.response.DownloadUrlResponse;
import com.likelion.cms.domain.certificate.service.CertificateService;
import com.likelion.cms.global.security.AccountStatusGuard;
import com.likelion.cms.global.security.CurrentUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}