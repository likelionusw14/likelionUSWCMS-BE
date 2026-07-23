package com.likelion.cms.domain.resource.dto.response;

import com.likelion.cms.common.type.PartType;
import com.likelion.cms.domain.resource.entity.LearningResource;
import com.likelion.cms.support.file.dto.response.FileAssetResponse;

import java.time.LocalDateTime;

public record LearningResourceResponse(
        Long resourceId,
        String title,
        Integer week,
        PartType targetPart,
        FileAssetResponse file,
        Long createdBy,
        Integer version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static LearningResourceResponse from(LearningResource resource) {
        return new LearningResourceResponse(
                resource.getResourceId(),
                resource.getTitle(),
                resource.getWeek(),
                resource.getTargetPart(),
                FileAssetResponse.from(resource.getFileAsset()),
                resource.getCreatedByUser().getUserId(),
                resource.getVersion(),
                resource.getCreatedAt(),
                resource.getUpdatedAt()
        );
    }
}
