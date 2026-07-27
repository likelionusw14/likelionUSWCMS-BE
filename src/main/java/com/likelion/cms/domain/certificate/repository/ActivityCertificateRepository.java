package com.likelion.cms.domain.certificate.repository;

import com.likelion.cms.domain.certificate.entity.ActivityCertificate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityCertificateRepository extends JpaRepository<ActivityCertificate, Long> {
}
