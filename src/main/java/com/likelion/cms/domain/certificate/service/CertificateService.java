package com.likelion.cms.domain.certificate.service;

import com.likelion.cms.domain.certificate.dto.response.CertificatePreviewResponse;
import com.likelion.cms.domain.certificate.dto.response.CertificateResponse;
import com.likelion.cms.domain.certificate.dto.response.DownloadUrlResponse;
import com.likelion.cms.domain.certificate.entity.ActivityCertificate;
import com.likelion.cms.domain.certificate.entity.CertificateIssueStatus;
import com.likelion.cms.domain.certificate.repository.ActivityCertificateRepository;
import com.likelion.cms.domain.cohort.entity.Cohort;
import com.likelion.cms.domain.cohort.repository.CohortRepository;
import com.likelion.cms.domain.user.entity.AppUser;
import com.likelion.cms.domain.user.repository.AppUserRepository;
import com.likelion.cms.global.exception.BusinessException;
import com.likelion.cms.global.exception.ErrorCode;
import com.likelion.cms.support.file.entity.FileAsset;
import com.likelion.cms.support.file.service.FileAssetService;
import com.likelion.cms.support.file.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CertificateService {

    private final ActivityCertificateRepository activityCertificateRepository;
    private final AppUserRepository appUserRepository;
    private final FileAssetService fileAssetService;
    private final CohortRepository cohortRepository;
    private final PdfGenerationService pdfGenerationService;

    public CertificatePreviewResponse previewMyCertificateData(Long userId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return CertificatePreviewResponse.from(user);
    }

    public DownloadUrlResponse getDownloadUrl(Long certificateId, Long userId) {
        ActivityCertificate certificate = activityCertificateRepository.findById(certificateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!certificate.getUser().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        if (certificate.getFileAsset() == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        FileStorage.PresignedDownload download =
                fileAssetService.createDownloadUrl(certificate.getFileAsset().getObjectKey());

        return DownloadUrlResponse.of(download.downloadUrl(), download.expiresAt());
    }

    @Transactional
    public CertificateResponse issueCertificate(Long userId, UUID idempotencyKey) {

        // 이미 같은 키로 발급된 요청이 있으면 기존 결과 그대로 반환 (중복 발급 방지)
        return activityCertificateRepository.findByIdempotencyKey(idempotencyKey)
                .map(CertificateResponse::from)
                .orElseGet(() -> createCertificate(userId, idempotencyKey));
    }

    private CertificateResponse createCertificate(Long userId, UUID idempotencyKey) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        Cohort cohort = cohortRepository.findById(user.getCohort().getCohortId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        LocalDateTime issuedAt = LocalDateTime.now();

        String activityType = user.getSystemRole().name().equals("ADMIN") ? "운영진" : "아기사자";

        byte[] pdfBytes = pdfGenerationService.generateCertificatePdf(
                user.getName(),
                user.getStudentId(),
                user.getDepartment(),
                cohort.getName(),
                activityType,
                user.getPart().name(),
                cohort.getStartedAt(),
                cohort.getEndedAt(),
                issuedAt
        );

        String fileName = "활동증명서_" + user.getName() + ".pdf";
        FileAsset fileAsset = fileAssetService.uploadPdf(pdfBytes, fileName, userId);

        ActivityCertificate certificate = ActivityCertificate.builder()
                .user(user)
                .cohort(cohort)
                .fileAsset(fileAsset)
                .issueStatus(CertificateIssueStatus.ISSUED)
                .nameSnapshot(user.getName())
                .departmentSnapshot(user.getDepartment())
                .studentIdSnapshot(user.getStudentId())
                .partSnapshot(user.getPart())
                .activityStartedAt(cohort.getStartedAt())
                .activityEndedAt(cohort.getEndedAt())
                .issuedAt(issuedAt)
                .idempotencyKey(idempotencyKey)
                .build();

        ActivityCertificate saved = activityCertificateRepository.save(certificate);

        return CertificateResponse.from(saved);
    }
}