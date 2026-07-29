package com.likelion.cms.domain.certificate.service;

import com.likelion.cms.common.type.PartType;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CertificateServiceTest {

    @Mock
    private ActivityCertificateRepository activityCertificateRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private FileAssetService fileAssetService;

    @Mock
    private CohortRepository cohortRepository;

    @Mock
    private PdfGenerationService pdfGenerationService;

    @Mock
    private CertificateIdempotencyStore idempotencyStore;

    @InjectMocks
    private CertificateService certificateService;

    private Cohort cohort;
    private AppUser user;

    @BeforeEach
    void setUp() {
        cohort = mock(Cohort.class);
        lenient().when(cohort.getCohortId()).thenReturn(1L);
        lenient().when(cohort.getNumber()).thenReturn(9);
        lenient().when(cohort.getName()).thenReturn("9기");

        user = mock(AppUser.class);
        lenient().when(user.getUserId()).thenReturn(1L);
        lenient().when(user.getName()).thenReturn("홍길동");
        lenient().when(user.getDepartment()).thenReturn("컴퓨터공학과");
        lenient().when(user.getStudentId()).thenReturn("20230001");
        lenient().when(user.getCohort()).thenReturn(cohort);
        lenient().when(user.getPart()).thenReturn(PartType.BACKEND);
        lenient().when(user.getSystemRole()).thenReturn(SystemRole.MEMBER);
    }

    @Test
    @DisplayName("존재하는 유저의 미리보기 정보를 정상 조회한다")
    void previewMyCertificateData_success() {
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));

        CertificatePreviewResponse response = certificateService.previewMyCertificateData(1L);

        assertThat(response.getName()).isEqualTo("홍길동");
        assertThat(response.getStudentId()).isEqualTo("20230001");
        assertThat(response.getCohort().getCohortId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("존재하지 않는 유저면 RESOURCE_NOT_FOUND 예외가 발생한다")
    void previewMyCertificateData_userNotFound_fails() {
        when(appUserRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> certificateService.previewMyCertificateData(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("본인 소유 증명서의 다운로드 URL을 정상 발급한다")
    void getDownloadUrl_success() {
        FileAsset fileAsset = mock(FileAsset.class);
        lenient().when(fileAsset.getObjectKey()).thenReturn("certificates/abc123.pdf");

        ActivityCertificate certificate = mock(ActivityCertificate.class);
        when(certificate.getUser()).thenReturn(user);
        when(certificate.getFileAsset()).thenReturn(fileAsset);

        FileStorage.PresignedDownload presignedDownload = new FileStorage.PresignedDownload(
                "https://s3.example.com/certificates/abc123.pdf?signature=xxx",
                OffsetDateTime.now().plusMinutes(10)
        );

        when(activityCertificateRepository.findById(10L)).thenReturn(Optional.of(certificate));
        when(fileAssetService.createDownloadUrl("certificates/abc123.pdf")).thenReturn(presignedDownload);

        DownloadUrlResponse response = certificateService.getDownloadUrl(10L, 1L);

        assertThat(response.getDownloadUrl()).isEqualTo("https://s3.example.com/certificates/abc123.pdf?signature=xxx");
    }

    @Test
    @DisplayName("존재하지 않는 certificateId면 RESOURCE_NOT_FOUND 예외가 발생한다")
    void getDownloadUrl_certificateNotFound_fails() {
        when(activityCertificateRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> certificateService.getDownloadUrl(999L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("본인 소유가 아닌 증명서에 접근하면 RESOURCE_NOT_FOUND(404) 예외가 발생한다")
    void getDownloadUrl_notOwner_fails() {
        AppUser otherUser = mock(AppUser.class);
        lenient().when(otherUser.getUserId()).thenReturn(2L);

        ActivityCertificate certificate = mock(ActivityCertificate.class);
        when(certificate.getUser()).thenReturn(otherUser);

        when(activityCertificateRepository.findById(10L)).thenReturn(Optional.of(certificate));

        assertThatThrownBy(() -> certificateService.getDownloadUrl(10L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("파일이 아직 없는 증명서(발급 실패 등)면 RESOURCE_NOT_FOUND 예외가 발생한다")
    void getDownloadUrl_noFileAsset_fails() {
        ActivityCertificate certificate = mock(ActivityCertificate.class);
        when(certificate.getUser()).thenReturn(user);
        when(certificate.getFileAsset()).thenReturn(null);

        when(activityCertificateRepository.findById(10L)).thenReturn(Optional.of(certificate));

        assertThatThrownBy(() -> certificateService.getDownloadUrl(10L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("증명서를 정상적으로 발급한다")
    void issueCertificate_success() {
        UUID idempotencyKey = UUID.randomUUID();
        FileAsset fileAsset = mock(FileAsset.class);
        lenient().when(fileAsset.getObjectKey()).thenReturn("certificates/new.pdf");

        when(idempotencyStore.find(1L, idempotencyKey)).thenReturn(Optional.empty());
        when(idempotencyStore.reserve(1L, idempotencyKey)).thenReturn(true);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cohortRepository.findById(1L)).thenReturn(Optional.of(cohort));
        when(pdfGenerationService.generateCertificatePdf(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), any()
        )).thenReturn(new byte[]{1, 2, 3});
        when(fileAssetService.uploadPdf(any(), anyString(), eq(1L))).thenReturn(fileAsset);

        ActivityCertificate saved = ActivityCertificate.builder()
                .user(user)
                .cohort(cohort)
                .fileAsset(fileAsset)
                .issueStatus(CertificateIssueStatus.ISSUED)
                .nameSnapshot("홍길동")
                .departmentSnapshot("컴퓨터공학과")
                .studentIdSnapshot("20230001")
                .partSnapshot(PartType.BACKEND)
                .issuedAt(java.time.LocalDateTime.now())
                .build();
        when(activityCertificateRepository.save(any())).thenReturn(saved);

        CertificateResponse response = certificateService.issueCertificate(1L, idempotencyKey);

        assertThat(response.getSnapshot().getName()).isEqualTo("홍길동");
        assertThat(response.getStatus()).isEqualTo(CertificateIssueStatus.ISSUED);
        verify(idempotencyStore).complete(eq(1L), eq(idempotencyKey), any());
    }

    @Test
    @DisplayName("이미 완료된 Idempotency-Key로 요청하면 기존 결과를 그대로 반환한다")
    void issueCertificate_idempotentReplay_returnsExisting() {
        UUID idempotencyKey = UUID.randomUUID();
        ActivityCertificate existingCertificate = ActivityCertificate.builder()
                .user(user)
                .cohort(cohort)
                .issueStatus(CertificateIssueStatus.ISSUED)
                .nameSnapshot("홍길동")
                .departmentSnapshot("컴퓨터공학과")
                .studentIdSnapshot("20230001")
                .partSnapshot(PartType.BACKEND)
                .issuedAt(java.time.LocalDateTime.now())
                .build();

        when(idempotencyStore.find(1L, idempotencyKey))
                .thenReturn(Optional.of(CertificateIdempotencyRecord.completed(10L)));
        when(activityCertificateRepository.findById(10L)).thenReturn(Optional.of(existingCertificate));

        CertificateResponse response = certificateService.issueCertificate(1L, idempotencyKey);

        assertThat(response.getSnapshot().getName()).isEqualTo("홍길동");
        verify(appUserRepository, never()).findById(anyLong());
        verify(pdfGenerationService, never()).generateCertificatePdf(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), any()
        );
    }

    @Test
    @DisplayName("동시에 같은 Idempotency-Key로 요청이 겹치면(PENDING) CONFLICT 예외가 발생한다")
    void issueCertificate_pendingConflict_fails() {
        UUID idempotencyKey = UUID.randomUUID();

        when(idempotencyStore.find(1L, idempotencyKey)).thenReturn(Optional.empty());
        when(idempotencyStore.reserve(1L, idempotencyKey)).thenReturn(false);
        when(idempotencyStore.find(1L, idempotencyKey))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(CertificateIdempotencyRecord.pending()));

        assertThatThrownBy(() -> certificateService.issueCertificate(1L, idempotencyKey))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
    }
}