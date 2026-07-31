package com.likelion.cms.domain.certificate.repository;

import com.likelion.cms.domain.certificate.entity.ActivityCertificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ActivityCertificateRepository extends JpaRepository<ActivityCertificate, Long> {

    Optional<ActivityCertificate> findByIdempotencyKey(UUID idempotencyKey);
}