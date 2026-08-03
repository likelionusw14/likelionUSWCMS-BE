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
import org.springframework.transaction.annotation.Propagation;
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

    // 🔴 클래스 레벨 @Transactional(readOnly = true)를 물려받으면 이 메서드 안에서 호출되는
    // fileAssetService.uploadPdf(...) 같은 쓰기 작업이 읽기 전용 트랜잭션에 갇혀 실패한다.
    // NOT_SUPPORTED로 트랜잭션 자체를 비활성화하고, 실제 DB 쓰기는 아래
    // transactionTemplate.execute(...) 블록에서 별도 트랜잭션으로 처리한다.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public CertificateResponse issueCertificate(Long userId, UUID idempotencyKey) {
        return idempotencyStore.find(userId, idempotencyKey)
                .map(record -> handleExistingRecord(record, userId))
                .orElseGet(() -> reserveAndCreate(userId, idempotencyKey));
    }

    private CertificateResponse handleExistingRecord(CertificateIdempotencyRecord record, Long userId) {
        if (record.status() == CertificateIdempotencyRecord.Status.PENDING) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        // 🟠 지연 로딩(cohort 등)을 트랜잭션이 살아있는 동안 안전하게 매핑하기 위해
        // 조회부터 응답 생성까지 짧은 트랜잭션 안에서 처리한다.
        return transactionTemplate.execute(status -> {
            ActivityCertificate certificate = activityCertificateRepository.findById(record.certificateId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
            return CertificateResponse.from(certificate);
        });
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

            // 🟠 3) user.getCohort()는 지연 로딩(LAZY)인데, issueCertificate 초반에 조회한 user는
            // NOT_SUPPORTED 트랜잭션(즉 트랜잭션 없음) 하에서 가져온 detached 상태다.
            // transactionTemplate이 만드는 REQUIRES_NEW 트랜잭션은 별도의 영속성 컨텍스트를 가지므로,
            // 그 안에서 user를 다시 조회(managed 상태로)한 뒤에 cohort를 지연 로딩해야 한다.
            Long cohortId = transactionTemplate.execute(status -> {
                AppUser managedUser = appUserRepository.findById(userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
                return managedUser.getCohort().getCohortId();
            });

            Cohort cohort = cohortRepository.findById(cohortId)
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

            // 🟠 2) DB save + 응답 매핑까지 짧은 독립 트랜잭션(REQUIRES_NEW)으로 감싼다.
            //  - issueCertificate를 감싼 클래스 레벨 readOnly 트랜잭션과 분리해 쓰기를 수행하고,
            //  - CertificateResponse.from(...)도 트랜잭션이 살아있는 동안 실행해 cohort 등
            //    지연 로딩 필드에 안전하게 접근한다.
            //  전파 설정은 TransactionConfig의 transactionTemplate 빈 참고.
            // 🟠 4) idempotencyStore.complete(...)는 DB 커밋이 실제로 끝난 뒤에 호출해야 한다.
            // execute(...) 블록 안에서 호출하면 커밋 전에 Redis가 COMPLETED로 표시되어,
            // 커밋이 실패할 경우 Redis만 완료 상태로 남는 불일치가 생긴다.
            // 따라서 save까지만 트랜잭션 안에서 수행하고, execute()가 반환된(=커밋된) 이후에
            // complete()를 호출한다. cohort는 빌더에서 직접 설정한 참조라 지연 로딩 문제가 없어
            // CertificateResponse.from(saved)는 트랜잭션 밖에서 호출해도 안전하다.
            ActivityCertificate saved = transactionTemplate.execute(status -> {
                ActivityCertificate certificate = ActivityCertificate.builder()
                        .user(user)
                        .cohort(cohort)
                        .fileAsset(fileAsset)
                        .issueStatus(CertificateIssueStatus.ISSUED)
                        .idempotencyKey(idempotencyKey)
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

            idempotencyStore.complete(userId, idempotencyKey, saved.getCertificateId());

            return CertificateResponse.from(saved);
        } catch (RuntimeException exception) {
            idempotencyStore.clearPending(userId, idempotencyKey);
            throw exception;
        }
    }
}
