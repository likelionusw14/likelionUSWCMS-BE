package com.likelion.cms.domain.certificate.service;

import com.likelion.cms.domain.certificate.repository.ActivityCertificateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CertificateService {

    private final ActivityCertificateRepository activityCertificateRepository;
}
