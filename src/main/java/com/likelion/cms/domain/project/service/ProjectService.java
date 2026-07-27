package com.likelion.cms.domain.project.service;

import com.likelion.cms.common.type.ProjectType;
import com.likelion.cms.domain.cohort.dto.response.CohortSummary;
import com.likelion.cms.domain.cohort.entity.Cohort;
import com.likelion.cms.domain.cohort.repository.CohortRepository;
import com.likelion.cms.domain.project.dto.request.CreateProjectRequest;
import com.likelion.cms.domain.project.dto.request.UpdateProjectRequest;
import com.likelion.cms.domain.project.dto.response.AdminProjectResponse;
import com.likelion.cms.domain.project.dto.response.ProjectParticipantResponse;
import com.likelion.cms.domain.project.dto.response.ProjectResponse;
import com.likelion.cms.domain.project.entity.Project;
import com.likelion.cms.domain.project.entity.ProjectParticipation;
import com.likelion.cms.domain.project.repository.ProjectParticipationRepository;
import com.likelion.cms.domain.project.repository.ProjectRepository;
import com.likelion.cms.domain.user.entity.AppUser;
import com.likelion.cms.domain.user.repository.AppUserRepository;
import com.likelion.cms.global.exception.BusinessException;
import com.likelion.cms.global.exception.ErrorCode;
import com.likelion.cms.global.response.PageMeta;
import com.likelion.cms.global.response.PageResponse;
import com.likelion.cms.support.file.dto.response.FileAssetResponse;
import com.likelion.cms.support.file.dto.response.FileView;
import com.likelion.cms.support.file.entity.FileAsset;
import com.likelion.cms.support.file.entity.FilePurpose;
import com.likelion.cms.support.file.repository.FileAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectParticipationRepository projectParticipationRepository;
    private final CohortRepository cohortRepository;
    private final FileAssetRepository fileAssetRepository;
    private final AppUserRepository appUserRepository;

    // ===== 여기부터 일반 조회 API (#3/#36, 다른 PR에서 구현) =====

    public PageResponse<ProjectResponse> getProjects(Long cohortId, List<ProjectType> projectTypes, int page, int size) {
        List<String> projectTypeNames = (projectTypes == null || projectTypes.isEmpty())
                ? null
                : projectTypes.stream().map(Enum::name).toList();

        Page<Project> projects = projectRepository.findProjects(cohortId, projectTypeNames, PageRequest.of(page, size));

        List<Long> projectIds = projects.getContent().stream().map(Project::getProjectId).toList();
        Map<Long, List<ProjectParticipantResponse>> participantsByProject = projectParticipationRepository
                .findByProjectIdsWithUser(projectIds).stream()
                .collect(Collectors.groupingBy(
                        participation -> participation.getProject().getProjectId(),
                        Collectors.mapping(this::toParticipantResponse, Collectors.toList())
                ));

        List<ProjectResponse> items = projects.getContent().stream()
                .map(project -> toResponse(project, participantsByProject.getOrDefault(project.getProjectId(), List.of())))
                .toList();

        return PageResponse.of(items, toPageMeta(projects));
    }

    public ProjectResponse getProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        List<ProjectParticipantResponse> participants = projectParticipationRepository
                .findByProjectIdWithUser(projectId).stream()
                .map(this::toParticipantResponse)
                .toList();

        return toResponse(project, participants);
    }

    private ProjectResponse toResponse(Project project, List<ProjectParticipantResponse> participants) {
        return ProjectResponse.of(
                project.getProjectId(),
                project.getTitle(),
                project.getDescription(),
                project.getDeployUrl(),
                project.getGithubUrl(),
                YearMonth.from(project.getStartedMonth()),
                YearMonth.from(project.getEndedMonth()),
                project.getVersion(),
                toFileView(project.getThumbnailAsset()),
                ProjectType.valueOf(project.getProjectType()),
                toCohortSummary(project.getCohort()),
                participants,
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }

    private ProjectParticipantResponse toParticipantResponse(ProjectParticipation participation) {
        return ProjectParticipantResponse.of(
                participation.getUser().getUserId(),
                participation.getUser().getName(),
                participation.getRole()
        );
    }

    private CohortSummary toCohortSummary(Cohort cohort) {
        return CohortSummary.of(cohort.getCohortId(), cohort.getNumber(), cohort.getName());
    }

    private FileView toFileView(FileAsset thumbnailAsset) {
        if (thumbnailAsset == null) {
            return null;
        }
        // presign 인프라가 아직 없어 downloadUrl/expiresAt은 비워둠
        return FileView.of(FileAssetResponse.from(thumbnailAsset), null, null);
    }

    private PageMeta toPageMeta(Page<?> page) {
        return PageMeta.of(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.hasNext());
    }

    // ===== 여기부터 관리자 등록/수정/삭제 API (이 이슈에서 구현) =====

    @Transactional
    public AdminProjectResponse create(CreateProjectRequest request, Long actorUserId) {
        AppUser actor = findActor(actorUserId);
        // cohortId는 필수 - 프로젝트는 반드시 특정 기수에 속해야 함 (엔티티에도 not-null).
        Cohort cohort = findCohort(request.cohortId());
        FileAsset thumbnailAsset = request.thumbnailAssetId() == null
                ? null
                : findThumbnailAsset(request.thumbnailAssetId());

        Project project = Project.builder()
                .title(request.title().trim())
                .description(request.description().trim())
                .projectType(request.projectType().name())
                .thumbnailAsset(thumbnailAsset)
                .deployUrl(request.deployUrl())
                .githubUrl(request.githubUrl())
                .cohort(cohort)
                .startedMonth(request.startedMonth())
                .endedMonth(request.endedMonth())
                .createdByUser(actor)
                .build();

        return AdminProjectResponse.from(projectRepository.save(project));
    }

    @Transactional
    public AdminProjectResponse update(Long projectId, UpdateProjectRequest request, Long actorUserId) {
        findActor(actorUserId);
        Project project = findProject(projectId);
        validateVersion(project.getVersion(), request.getVersion());

        // 부분 수정 패턴 (UserService.update와 동일한 방식)
        if (request.isTitleProvided()) {
            project.updateTitle(request.getTitle().trim());
        }
        if (request.isDescriptionProvided()) {
            project.updateDescription(request.getDescription().trim());
        }
        if (request.isProjectTypeProvided()) {
            project.updateProjectType(request.getProjectType().name());
        }
        // thumbnailAssetId: 키 자체를 안 보내면(false) 기존 값 유지,
        // "thumbnailAssetId": null 로 명시적으로 보내면 썸네일 제거,
        // 실제 ID를 보내면 그 파일로 교체.
        if (request.isThumbnailAssetIdProvided()) {
            FileAsset thumbnailAsset = request.getThumbnailAssetId() == null
                    ? null
                    : findThumbnailAsset(request.getThumbnailAssetId());
            project.updateThumbnailAsset(thumbnailAsset);
        }
        if (request.isDeployUrlProvided()) {
            project.updateDeployUrl(request.getDeployUrl());
        }
        if (request.isGithubUrlProvided()) {
            project.updateGithubUrl(request.getGithubUrl());
        }
        if (request.isCohortIdProvided()) {
            project.updateCohort(findCohort(request.getCohortId()));
        }
        if (request.isStartedMonthProvided()) {
            project.updateStartedMonth(request.getStartedMonth());
        }
        if (request.isEndedMonthProvided()) {
            project.updateEndedMonth(request.getEndedMonth());
        }
        // 두 날짜 중 하나만 바뀌어도 "종료일 < 시작일"이 될 수 있어서,
        // 모든 필드 반영이 끝난 "최종 상태" 기준으로 마지막에 한 번 더 검증.
        validateDateRange(project.getStartedMonth(), project.getEndedMonth());

        // @Version은 실제 UPDATE가 나가야(flush) 엔티티의 메모리 값도 올라감.
        // flush 없이 바로 응답을 만들면 version이 갱신 전 값으로 내려가서,
        // 클라이언트가 그 값으로 바로 다음 PATCH를 보내면 낙관적 락 충돌이 남.
        projectRepository.flush();
        return AdminProjectResponse.from(project);
    }

    @Transactional
    public void delete(Long projectId, Long actorUserId) {
        findActor(actorUserId);
        Project project = findProject(projectId);
        project.delete();
    }

    private AppUser findActor(Long actorUserId) {
        return appUserRepository.findById(actorUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    private Project findProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private Cohort findCohort(Long cohortId) {
        return cohortRepository.findById(cohortId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    // 파일이 존재하는지뿐 아니라 "프로젝트 썸네일 용도로 업로드된 파일인지"까지 검증.
    // 공지 이미지용으로 올린 파일을 프로젝트 썸네일에 잘못 끼워 넣는 걸 막기 위함.
    private FileAsset findThumbnailAsset(Long fileAssetId) {
        FileAsset fileAsset = fileAssetRepository.findById(fileAssetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (fileAsset.getPurpose() != FilePurpose.PROJECT_THUMBNAIL) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "프로젝트 썸네일 용도의 파일만 사용할 수 있습니다.");
        }
        return fileAsset;
    }

    private void validateVersion(Integer actualVersion, Integer requestedVersion) {
        if (!requestedVersion.equals(actualVersion)) {
            throw new BusinessException(ErrorCode.OPTIMISTIC_LOCK_CONFLICT);
        }
    }

    private void validateDateRange(LocalDate startedMonth, LocalDate endedMonth) {
        if (endedMonth.isBefore(startedMonth)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "endedMonth는 startedMonth보다 빠를 수 없습니다.");
        }
    }
}
