package com.likelion.cms.domain.certificate.service;

import com.likelion.cms.domain.certificate.dto.response.CertificatePreviewResponse;
import com.likelion.cms.domain.certificate.dto.response.DownloadUrlResponse;
import com.likelion.cms.domain.certificate.entity.ActivityCertificate;
import com.likelion.cms.domain.certificate.repository.ActivityCertificateRepository;
import com.likelion.cms.domain.user.entity.AppUser;
import com.likelion.cms.domain.user.repository.AppUserRepository;
import com.likelion.cms.global.exception.BusinessException;
import com.likelion.cms.global.exception.ErrorCode;
import com.likelion.cms.support.file.service.FileAssetService;
import com.likelion.cms.support.file.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CertificateService {

    private final ActivityCertificateRepository activityCertificateRepository;
    private final AppUserRepository appUserRepository;
    private final FileAssetService fileAssetService;

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
}