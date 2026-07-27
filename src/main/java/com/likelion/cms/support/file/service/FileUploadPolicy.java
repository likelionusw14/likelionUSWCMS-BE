package com.likelion.cms.support.file.service;

import com.likelion.cms.global.exception.BusinessException;
import com.likelion.cms.global.exception.ErrorCode;
import com.likelion.cms.support.file.entity.FilePurpose;
import org.springframework.stereotype.Component;

import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class FileUploadPolicy {

    static final long IMAGE_MAX_SIZE_BYTES = 5L * 1024 * 1024;
    static final long PDF_MAX_SIZE_BYTES = 50L * 1024 * 1024;

    private static final String PDF = "application/pdf";
    private static final Set<String> IMAGE_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final Map<String, Set<String>> ALLOWED_EXTENSIONS = Map.of(
            PDF, Set.of("pdf"),
            "image/jpeg", Set.of("jpg", "jpeg"),
            "image/png", Set.of("png"),
            "image/webp", Set.of("webp")
    );
    private static final Map<String, String> CANONICAL_EXTENSIONS = Map.of(
            PDF, "pdf",
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    public ValidatedFile validate(FilePurpose purpose, String originalFileName,
                                  String mimeType, long sizeBytes, String checksumSha256) {
        if (!isAllowedForPurpose(purpose, mimeType)
                || !hasAllowedExtension(originalFileName, mimeType)) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }

        long maximumSize = PDF.equals(mimeType) ? PDF_MAX_SIZE_BYTES : IMAGE_MAX_SIZE_BYTES;
        if (sizeBytes > maximumSize) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }

        String normalizedChecksum = checksumSha256 == null
                ? null
                : checksumSha256.toLowerCase(Locale.ROOT);
        return new ValidatedFile(mimeType, sizeBytes, normalizedChecksum,
                CANONICAL_EXTENSIONS.get(mimeType));
    }

    public String checksumBase64(String checksumSha256) {
        if (checksumSha256 == null) {
            return null;
        }
        return java.util.Base64.getEncoder()
                .encodeToString(HexFormat.of().parseHex(checksumSha256));
    }

    private boolean isAllowedForPurpose(FilePurpose purpose, String mimeType) {
        if (purpose == null || mimeType == null) {
            return false;
        }
        return switch (purpose) {
            case LEARNING_RESOURCE -> PDF.equals(mimeType) || IMAGE_MIME_TYPES.contains(mimeType);
            case PROJECT_THUMBNAIL, NOTICE_IMAGE -> IMAGE_MIME_TYPES.contains(mimeType);
            case CERTIFICATE -> PDF.equals(mimeType);
        };
    }

    private boolean hasAllowedExtension(String originalFileName, String mimeType) {
        if (originalFileName == null) {
            return false;
        }
        int lastDot = originalFileName.lastIndexOf('.');
        if (lastDot < 0 || lastDot == originalFileName.length() - 1) {
            return false;
        }
        String extension = originalFileName.substring(lastDot + 1).toLowerCase(Locale.ROOT);
        return ALLOWED_EXTENSIONS.getOrDefault(mimeType, Set.of()).contains(extension);
    }

    public record ValidatedFile(
            String mimeType,
            long sizeBytes,
            String checksumSha256,
            String canonicalExtension
    ) {
    }
}
