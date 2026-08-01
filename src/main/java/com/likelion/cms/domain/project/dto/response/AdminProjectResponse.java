package com.likelion.cms.domain.project.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.likelion.cms.common.type.ProjectType;
import com.likelion.cms.domain.cohort.dto.response.CohortSummary;
import com.likelion.cms.domain.project.entity.Project;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

// 관리자 프로젝트 등록/수정 API 전용 응답. 같은 패키지의 ProjectResponse는
// feature/3(일반 조회 API)가 이미 구현한 별도 클래스라 이름이 겹침 - 필드 구성이
// 조금 달라서(thumbnail 표현 등) 하나로 합치지 않고 분리 유지.
// projectType은 원래 String(엔티티 그대로)이었는데, 유효하지 않은 값이 그대로 저장돼서
// 조회 API에서 500이 나는 걸 실서버 검증 중 발견 - ProjectResponse와 동일하게
// ProjectType enum으로 노출하도록 맞춤 (엔티티 저장은 여전히 String, 변환은 from()에서).
// startedMonth/endedMonth는 docs/CONVENTIONS.md 3-2절 기준으로 YearMonth를 쓰고,
// 엔티티(LocalDate)와의 변환도 이 from() 안에서 처리한다. 요청도 같은 YearMonth라
// 조회한 값을 그대로 다시 PATCH에 넣을 수 있다.
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminProjectResponse {

    private final Long projectId;
    private final String title;
    private final String description;
    private final ProjectType projectType;
    private final Long thumbnailAssetId;
    private final String deployUrl;
    private final String githubUrl;
    private final CohortSummary cohort;
    private final YearMonth startedMonth;
    private final YearMonth endedMonth;
    private final List<ProjectParticipantResponse> participants;
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
            Long projectId, String title, String description, ProjectType projectType, Long thumbnailAssetId,
            String deployUrl, String githubUrl, CohortSummary cohort, YearMonth startedMonth, YearMonth endedMonth,
            List<ProjectParticipantResponse> participants, Long createdBy, Integer version,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new AdminProjectResponse(projectId, title, description, projectType, thumbnailAssetId,
                deployUrl, githubUrl, cohort, startedMonth, endedMonth, participants, createdBy, version,
                createdAt, updatedAt);
    }

    // 참여자는 엔티티에서 lazy로 끌어오면 N+1이 나서, 서비스가 별도로 조회한 목록을 받는다.
    public static AdminProjectResponse from(Project project, List<ProjectParticipantResponse> participants) {
        return of(
                project.getProjectId(),
                project.getTitle(),
                project.getDescription(),
                ProjectType.valueOf(project.getProjectType()),
                project.getThumbnailAsset() == null ? null : project.getThumbnailAsset().getFileAssetId(),
                project.getDeployUrl(),
                project.getGithubUrl(),
                CohortSummary.from(project.getCohort()),
                YearMonth.from(project.getStartedMonth()),
                YearMonth.from(project.getEndedMonth()),
                participants,
                project.getCreatedByUser().getUserId(),
                project.getVersion(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
