package com.likelion.cms.domain.project.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.likelion.cms.domain.cohort.dto.response.CohortSummary;
import com.likelion.cms.domain.project.entity.Project;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.YearMonth;

// 관리자 프로젝트 등록/수정 API 전용 응답. 같은 패키지의 ProjectResponse는
// feature/3(일반 조회 API)가 이미 선점한 이름이라 이름이 겹침 - 그쪽은 ProjectType
// enum 기준(아직 미구현 스캐폴딩)이고, 이쪽은 실제 Project 엔티티 필드 타입
// (String projectType) 그대로 노출하는 게 달라서 하나로 합치지 않음.
// startedMonth/endedMonth는 docs/CONVENTIONS.md 3-2절 기준으로 YearMonth를 쓰고,
// 엔티티(LocalDate)와의 변환은 이 from() 안에서 처리한다.
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminProjectResponse {

    private final Long projectId;
    private final String title;
    private final String description;
    private final String projectType;
    private final Long thumbnailAssetId;
    private final String deployUrl;
    private final String githubUrl;
    private final CohortSummary cohort;
    private final YearMonth startedMonth;
    private final YearMonth endedMonth;
    private final Long createdBy;

    /**
     * version : 낙관적 락(optimistic locking)을 위한 버전 값입니다.
     * 수정 요청 시 클라이언트가 조회 시점의 이 값을 그대로 전달해야 하며,
     * 서버에 저장된 현재 버전과 다르면 충돌로 간주해 요청이 거부됩니다.
     */
    private final Integer version;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static AdminProjectResponse of(
            Long projectId, String title, String description, String projectType, Long thumbnailAssetId,
            String deployUrl, String githubUrl, CohortSummary cohort, YearMonth startedMonth, YearMonth endedMonth,
            Long createdBy, Integer version, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new AdminProjectResponse(projectId, title, description, projectType, thumbnailAssetId,
                deployUrl, githubUrl, cohort, startedMonth, endedMonth, createdBy, version, createdAt, updatedAt);
    }

    public static AdminProjectResponse from(Project project) {
        return of(
                project.getProjectId(),
                project.getTitle(),
                project.getDescription(),
                project.getProjectType(),
                project.getThumbnailAsset() == null ? null : project.getThumbnailAsset().getFileAssetId(),
                project.getDeployUrl(),
                project.getGithubUrl(),
                CohortSummary.from(project.getCohort()),
                YearMonth.from(project.getStartedMonth()),
                YearMonth.from(project.getEndedMonth()),
                project.getCreatedByUser().getUserId(),
                project.getVersion(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
