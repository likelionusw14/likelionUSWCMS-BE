package com.likelion.cms.domain.certificate.controller;

import com.likelion.cms.domain.certificate.service.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/certificates")
public class CertificateController {

    private final CertificateService certificateService;
}
