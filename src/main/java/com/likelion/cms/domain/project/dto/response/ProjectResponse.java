package com.likelion.cms.domain.project.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.likelion.cms.domain.cohort.dto.response.CohortSummary;
import com.likelion.cms.domain.project.entity.Project;

import java.time.LocalDate;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProjectResponse(
        Long projectId,
        String title,
        String description,
        String projectType,
        Long thumbnailAssetId,
        String deployUrl,
        String githubUrl,
        CohortSummary cohort,
        LocalDate startedMonth,
        LocalDate endedMonth,
        Long createdBy,
        Integer version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getProjectId(),
                project.getTitle(),
                project.getDescription(),
                project.getProjectType(),
                project.getThumbnailAsset() == null ? null : project.getThumbnailAsset().getFileAssetId(),
                project.getDeployUrl(),
                project.getGithubUrl(),
                CohortSummary.from(project.getCohort()),
                project.getStartedMonth(),
                project.getEndedMonth(),
                project.getCreatedByUser().getUserId(),
                project.getVersion(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
