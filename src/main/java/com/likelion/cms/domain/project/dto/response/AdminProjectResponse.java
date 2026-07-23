package com.likelion.cms.domain.project.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.likelion.cms.domain.cohort.dto.response.CohortSummary;
import com.likelion.cms.domain.project.entity.Project;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 관리자 프로젝트 등록/수정 API 전용 응답. 같은 패키지의 ProjectResponse는
// feature/3(일반 조회 API)가 이미 선점한 이름이라 이름이 겹침 - 그쪽은 ProjectType
// enum/YearMonth 기준(아직 미구현 스캐폴딩)이고, 이쪽은 실제 Project 엔티티 필드
// 타입(String projectType, LocalDate) 그대로 노출하는 게 달라서 하나로 합치지 않음.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdminProjectResponse(
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
    public static AdminProjectResponse from(Project project) {
        return new AdminProjectResponse(
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
