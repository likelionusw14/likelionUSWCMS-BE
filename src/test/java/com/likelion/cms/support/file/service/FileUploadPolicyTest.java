package com.likelion.cms.support.file.service;

import com.likelion.cms.global.exception.BusinessException;
import com.likelion.cms.global.exception.ErrorCode;
import com.likelion.cms.support.file.entity.FilePurpose;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileUploadPolicyTest {

    private final FileUploadPolicy policy = new FileUploadPolicy();

    @Test
    void acceptsLearningResourcePdfAtMaximumSize() {
        FileUploadPolicy.ValidatedFile file = policy.validate(
                FilePurpose.LEARNING_RESOURCE,
                "lecture.PDF",
                "application/pdf",
                FileUploadPolicy.PDF_MAX_SIZE_BYTES,
                "A".repeat(64)
        );

        assertThat(file.canonicalExtension()).isEqualTo("pdf");
        assertThat(file.checksumSha256()).isEqualTo("a".repeat(64));
    }

    @Test
    void acceptsSupportedLearningResourceImages() {
        assertThat(policy.validate(
                FilePurpose.LEARNING_RESOURCE,
                "preview.jpeg",
                "image/jpeg",
                FileUploadPolicy.IMAGE_MAX_SIZE_BYTES,
                null
        ).canonicalExtension()).isEqualTo("jpg");
        assertThat(policy.validate(
                FilePurpose.LEARNING_RESOURCE,
                "preview.png",
                "image/png",
                1L,
                null
        ).canonicalExtension()).isEqualTo("png");
        assertThat(policy.validate(
                FilePurpose.LEARNING_RESOURCE,
                "preview.webp",
                "image/webp",
                1L,
                null
        ).canonicalExtension()).isEqualTo("webp");
    }

    @Test
    void rejectsFileOverMimeSpecificLimit() {
        assertThatThrownBy(() -> policy.validate(
                FilePurpose.LEARNING_RESOURCE,
                "preview.png",
                "image/png",
                FileUploadPolicy.IMAGE_MAX_SIZE_BYTES + 1,
                null
        )).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FILE_TOO_LARGE));
    }

    @Test
    void rejectsMimeAndExtensionMismatch() {
        assertThatThrownBy(() -> policy.validate(
                FilePurpose.LEARNING_RESOURCE,
                "document.png",
                "application/pdf",
                1024L,
                null
        )).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.UNSUPPORTED_FILE_TYPE));
    }

    @Test
    void rejectsSvgAndGif() {
        assertUnsupported("vector.svg", "image/svg+xml");
        assertUnsupported("animated.gif", "image/gif");
    }

    @Test
    void restrictsPurposeSpecificMimeTypes() {
        assertThatThrownBy(() -> policy.validate(
                FilePurpose.PROJECT_THUMBNAIL,
                "document.pdf",
                "application/pdf",
                1024L,
                null
        )).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.UNSUPPORTED_FILE_TYPE));
    }

    @Test
    void convertsHexSha256ToBase64() {
        assertThat(policy.checksumBase64("00".repeat(32)))
                .isEqualTo("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
    }

    private void assertUnsupported(String fileName, String mimeType) {
        assertThatThrownBy(() -> policy.validate(
                FilePurpose.LEARNING_RESOURCE,
                fileName,
                mimeType,
                1024L,
                null
        )).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.UNSUPPORTED_FILE_TYPE));
    }
}
