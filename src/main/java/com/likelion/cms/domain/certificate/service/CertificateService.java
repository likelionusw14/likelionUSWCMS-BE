package com.likelion.cms.domain.certificate.service;

import com.likelion.cms.domain.certificate.dto.response.CertificatePreviewResponse;
import com.likelion.cms.domain.certificate.dto.response.CertificateResponse;
import com.likelion.cms.domain.certificate.dto.response.DownloadUrlResponse;
import com.likelion.cms.domain.certificate.entity.ActivityCertificate;
import com.likelion.cms.domain.certificate.entity.CertificateIssueStatus;
import com.likelion.cms.domain.certificate.repository.ActivityCertificateRepository;
import com.likelion.cms.domain.certificate.store.CertificateIdempotencyRecord;
import com.likelion.cms.domain.certificate.store.CertificateIdempotencyStore;
import com.likelion.cms.domain.cohort.entity.Cohort;
import com.likelion.cms.domain.cohort.repository.CohortRepository;
import com.likelion.cms.domain.user.entity.AppUser;
import com.likelion.cms.domain.user.entity.SystemRole;
import com.likelion.cms.domain.user.repository.AppUserRepository;
import com.likelion.cms.global.exception.BusinessException;
import com.likelion.cms.global.exception.ErrorCode;
import com.likelion.cms.support.file.entity.FileAsset;
import com.likelion.cms.support.file.service.FileAssetService;
import com.likelion.cms.support.file.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

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
    private final CertificateIdempotencyStore idempotencyStore;
    private final TransactionTemplate transactionTemplate;

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

    // 🟠 더 이상 전체를 @Transactional로 감싸지 않습니다.
    // 쓰기(entity save)는 아래 createCertificate 내부의 transactionTemplate 블록에서만 짧게 처리됩니다.
    public CertificateResponse issueCertificate(Long userId, UUID idempotencyKey) {
        return idempotencyStore.find(userId, idempotencyKey)
                .map(record -> handleExistingRecord(record, userId))
                .orElseGet(() -> reserveAndCreate(userId, idempotencyKey));
    }

    private CertificateResponse handleExistingRecord(CertificateIdempotencyRecord record, Long userId) {
        if (record.status() == CertificateIdempotencyRecord.Status.PENDING) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        ActivityCertificate certificate = activityCertificateRepository.findById(record.certificateId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return CertificateResponse.from(certificate);
    }

    private CertificateResponse reserveAndCreate(Long userId, UUID idempotencyKey) {
        boolean reserved = idempotencyStore.reserve(userId, idempotencyKey);
        if (!reserved) {
            CertificateIdempotencyRecord record = idempotencyStore.find(userId, idempotencyKey)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT));
            return handleExistingRecord(record, userId);
        }
        return createCertificate(userId, idempotencyKey);
    }

    private CertificateResponse createCertificate(Long userId, UUID idempotencyKey) {
        // 🟠 1) 실패 시 clearPending 호출 후 rethrow — 10분 잠김 문제 해결
        try {
            AppUser user = appUserRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

            Cohort cohort = cohortRepository.findById(user.getCohort().getCohortId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

            LocalDateTime issuedAt = LocalDateTime.now();

            String activityType = user.getSystemRole() == SystemRole.ADMIN ? "운영진" : "아기사자";

            // 🟠 2) PDF 생성 + S3 업로드는 트랜잭션 밖에서 실행
            byte[] pdfBytes = pdfGenerationService.generateCertificatePdf(
                    user.getName(),
                    user.getStudentId(),
                    user.getDepartment(),
                    cohort.getName(),
                    activityType,
                    user.getPart().name(),
                    issuedAt
            );

            String fileName = "활동증명서_" + user.getName() + ".pdf";
            FileAsset fileAsset = fileAssetService.uploadPdf(pdfBytes, fileName, userId);

            // 🟠 2) DB save만 짧은 트랜잭션으로 감싸기
            ActivityCertificate saved = transactionTemplate.execute(status -> {
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
                        .build();
                return activityCertificateRepository.save(certificate);
            });

            // DB save가 성공적으로 커밋된 뒤에만 Redis를 COMPLETED로 표시
            idempotencyStore.complete(userId, idempotencyKey, saved.getCertificateId());

            return CertificateResponse.from(saved);
        } catch (RuntimeException exception) {
            idempotencyStore.clearPending(userId, idempotencyKey);
            throw exception;
        }
    }
}
