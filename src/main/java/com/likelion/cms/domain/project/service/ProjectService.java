package com.likelion.cms.domain.project.service;

import com.likelion.cms.domain.cohort.entity.Cohort;
import com.likelion.cms.domain.cohort.repository.CohortRepository;
import com.likelion.cms.domain.project.dto.request.CreateProjectRequest;
import com.likelion.cms.domain.project.dto.request.UpdateProjectRequest;
import com.likelion.cms.domain.project.dto.response.ProjectResponse;
import com.likelion.cms.domain.project.entity.Project;
import com.likelion.cms.domain.project.repository.ProjectRepository;
import com.likelion.cms.domain.user.entity.AppUser;
import com.likelion.cms.domain.user.repository.AppUserRepository;
import com.likelion.cms.global.exception.BusinessException;
import com.likelion.cms.global.exception.ErrorCode;
import com.likelion.cms.support.file.entity.FileAsset;
import com.likelion.cms.support.file.entity.FilePurpose;
import com.likelion.cms.support.file.repository.FileAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final CohortRepository cohortRepository;
    private final FileAssetRepository fileAssetRepository;
    private final AppUserRepository appUserRepository;

    @Transactional
    public ProjectResponse create(CreateProjectRequest request, Long actorUserId) {
        AppUser actor = findActor(actorUserId);
        Cohort cohort = findCohort(request.cohortId());
        FileAsset thumbnailAsset = request.thumbnailAssetId() == null
                ? null
                : findThumbnailAsset(request.thumbnailAssetId());

        Project project = Project.builder()
                .title(request.title().trim())
                .description(request.description().trim())
                .projectType(request.projectType().trim())
                .thumbnailAsset(thumbnailAsset)
                .deployUrl(request.deployUrl())
                .githubUrl(request.githubUrl())
                .cohort(cohort)
                .startedMonth(request.startedMonth())
                .endedMonth(request.endedMonth())
                .createdByUser(actor)
                .build();

        return ProjectResponse.from(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponse update(Long projectId, UpdateProjectRequest request, Long actorUserId) {
        findActor(actorUserId);
        Project project = findProject(projectId);
        validateVersion(project.getVersion(), request.getVersion());

        if (request.isTitleProvided()) {
            project.updateTitle(request.getTitle().trim());
        }
        if (request.isDescriptionProvided()) {
            project.updateDescription(request.getDescription().trim());
        }
        if (request.isProjectTypeProvided()) {
            project.updateProjectType(request.getProjectType().trim());
        }
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
        validateDateRange(project.getStartedMonth(), project.getEndedMonth());

        return ProjectResponse.from(project);
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
