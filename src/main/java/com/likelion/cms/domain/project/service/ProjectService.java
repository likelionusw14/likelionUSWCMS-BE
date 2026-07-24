package com.likelion.cms.domain.project.service;

import com.likelion.cms.common.type.ProjectType;
import com.likelion.cms.domain.cohort.dto.response.CohortSummary;
import com.likelion.cms.domain.cohort.entity.Cohort;
import com.likelion.cms.domain.project.dto.response.ProjectParticipantResponse;
import com.likelion.cms.domain.project.dto.response.ProjectResponse;
import com.likelion.cms.domain.project.entity.Project;
import com.likelion.cms.domain.project.entity.ProjectParticipation;
import com.likelion.cms.domain.project.repository.ProjectParticipationRepository;
import com.likelion.cms.domain.project.repository.ProjectRepository;
import com.likelion.cms.global.exception.BusinessException;
import com.likelion.cms.global.exception.ErrorCode;
import com.likelion.cms.global.response.PageMeta;
import com.likelion.cms.global.response.PageResponse;
import com.likelion.cms.support.file.dto.response.FileAssetResponse;
import com.likelion.cms.support.file.dto.response.FileView;
import com.likelion.cms.support.file.entity.FileAsset;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectParticipationRepository projectParticipationRepository;

    public PageResponse<ProjectResponse> getProjects(Long cohortId, List<ProjectType> projectTypes, int page, int size) {
        List<String> projectTypeNames = (projectTypes == null || projectTypes.isEmpty())
                ? null
                : projectTypes.stream().map(Enum::name).toList();

        Page<Project> projects = projectRepository.findProjects(cohortId, projectTypeNames, PageRequest.of(page, size));

        List<Long> projectIds = projects.getContent().stream().map(Project::getProjectId).toList();
        Map<Long, List<ProjectParticipantResponse>> participantsByProject = projectParticipationRepository
                .findByProject_ProjectIdIn(projectIds).stream()
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
                .findByProject_ProjectId(projectId).stream()
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
                project.getCreatedAt().atOffset(ZoneOffset.UTC),
                project.getUpdatedAt().atOffset(ZoneOffset.UTC)
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
}
