package com.likelion.cms.support.file.store;

public record FileIdempotencyRecord(
        Status status,
        String objectKey,
        Long fileAssetId
) {

    public static FileIdempotencyRecord pending(String objectKey) {
        return new FileIdempotencyRecord(Status.PENDING, objectKey, null);
    }

    public static FileIdempotencyRecord completed(String objectKey, Long fileAssetId) {
        return new FileIdempotencyRecord(Status.COMPLETED, objectKey, fileAssetId);
    }

    public enum Status {
        PENDING,
        COMPLETED
    }
}
