package com.likelion.cms.support.file.dto.request;

import com.likelion.cms.support.file.entity.FilePurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record FileUploadUrlRequest(
        @NotNull FilePurpose purpose,
        @NotBlank
        @Size(max = 255)
        @Pattern(regexp = "^[^/\\\\\\p{Cntrl}]+$")
        String originalFileName,
        @NotBlank @Size(max = 150) String mimeType,
        @NotNull @Positive Long sizeBytes,
        @Pattern(regexp = "^[A-Fa-f0-9]{64}$") String checksumSha256
) {
}
