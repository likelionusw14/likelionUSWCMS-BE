package com.likelion.cms.domain.certificate.store;

public record CertificateIdempotencyRecord(
        Status status,
        Long certificateId
) {

    public static CertificateIdempotencyRecord pending() {
        return new CertificateIdempotencyRecord(Status.PENDING, null);
    }

    public static CertificateIdempotencyRecord completed(Long certificateId) {
        return new CertificateIdempotencyRecord(Status.COMPLETED, certificateId);
    }

    public enum Status {
        PENDING,
        COMPLETED
    }
}