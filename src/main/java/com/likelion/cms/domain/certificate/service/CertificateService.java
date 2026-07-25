package com.likelion.cms.domain.certificate.service;

import com.likelion.cms.domain.certificate.dto.response.CertificatePreviewResponse;
import com.likelion.cms.domain.certificate.repository.ActivityCertificateRepository;
import com.likelion.cms.domain.user.entity.AppUser;
import com.likelion.cms.domain.user.repository.AppUserRepository;
import com.likelion.cms.global.exception.BusinessException;
import com.likelion.cms.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CertificateService {

    private final ActivityCertificateRepository activityCertificateRepository;
    private final AppUserRepository appUserRepository;

    public CertificatePreviewResponse previewMyCertificateData(Long userId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return CertificatePreviewResponse.from(user);
    }
}