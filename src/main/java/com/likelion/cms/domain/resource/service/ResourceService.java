package com.likelion.cms.domain.resource.service;

import com.likelion.cms.common.type.PartType;
import com.likelion.cms.domain.resource.dto.request.CreateLearningResourceRequest;
import com.likelion.cms.domain.resource.dto.request.UpdateLearningResourceRequest;
import com.likelion.cms.domain.resource.dto.response.LearningResourceResponse;
import com.likelion.cms.domain.resource.entity.LearningResource;
import com.likelion.cms.domain.resource.repository.LearningResourceRepository;
import com.likelion.cms.domain.user.entity.AppUser;
import com.likelion.cms.domain.user.repository.AppUserRepository;
import com.likelion.cms.global.exception.BusinessException;
import com.likelion.cms.global.exception.ErrorCode;
import com.likelion.cms.global.response.PageMeta;
import com.likelion.cms.global.response.PageResponse;
import com.likelion.cms.support.file.entity.FileAsset;
import com.likelion.cms.support.file.entity.FilePurpose;
import com.likelion.cms.support.file.repository.FileAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResourceService {

    private final LearningResourceRepository learningResourceRepository;
    private final FileAssetRepository fileAssetRepository;
    private final AppUserRepository appUserRepository;

    @Transactional
    public LearningResourceResponse create(CreateLearningResourceRequest request, Long actorUserId) {
        AppUser actor = findActor(actorUserId);
        FileAsset fileAsset = findLearningResourceFile(request.fileAssetId());

        LearningResource resource = LearningResource.builder()
                .title(request.title().trim())
                .week(request.week())
                .targetPart(request.targetPart())
                .fileAsset(fileAsset)
                .createdByUser(actor)
                .build();

        return LearningResourceResponse.from(learningResourceRepository.save(resource));
    }

    @Transactional
    public LearningResourceResponse update(
            Long resourceId,
            UpdateLearningResourceRequest request,
            Long actorUserId
    ) {
        findActor(actorUserId);
        LearningResource resource = learningResourceRepository.findById(resourceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        validateVersion(resource.getVersion(), request.getVersion());

        if (request.isTitleProvided()) {
            resource.updateTitle(request.getTitle().trim());
        }
        if (request.isWeekProvided()) {
            resource.updateWeek(request.getWeek());
        }
        if (request.isTargetPartProvided()) {
            resource.updateTargetPart(request.getTargetPart());
        }
        if (request.isFileAssetIdProvided()) {
            resource.updateFileAsset(findLearningResourceFile(request.getFileAssetId()));
        }

        // @Version은 실제 UPDATE(flush) 시점에 증가한다. flush 없이 응답을 만들면
        // 갱신 전 version이 내려가 클라이언트의 다음 수정이 낙관적 락 충돌을 낸다.
        learningResourceRepository.flush();
        return LearningResourceResponse.from(resource);
    }

    public PageResponse<LearningResourceResponse> getResources(Integer week, PartType targetPart, int page, int size) {
        Page<LearningResource> resources = learningResourceRepository.findResources(week, targetPart, PageRequest.of(page, size));

        List<LearningResourceResponse> items = resources.getContent().stream()
                .map(LearningResourceResponse::from)
                .toList();

        return PageResponse.of(items, toPageMeta(resources));
    }

    public LearningResourceResponse getResource(Long resourceId) {
        LearningResource resource = learningResourceRepository.findById(resourceId)
                .filter(candidate -> candidate.getArchivedAt() == null)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        return LearningResourceResponse.from(resource);
    }

    private PageMeta toPageMeta(Page<?> page) {
        return PageMeta.of(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.hasNext());
    }

    private AppUser findActor(Long actorUserId) {
        return appUserRepository.findById(actorUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    private FileAsset findLearningResourceFile(Long fileAssetId) {
        FileAsset fileAsset = fileAssetRepository.findById(fileAssetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (fileAsset.getPurpose() != FilePurpose.LEARNING_RESOURCE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "학습 자료 용도의 파일만 사용할 수 있습니다.");
        }
        return fileAsset;
    }

    private void validateVersion(Integer actualVersion, Integer requestedVersion) {
        if (!requestedVersion.equals(actualVersion)) {
            throw new BusinessException(ErrorCode.OPTIMISTIC_LOCK_CONFLICT);
        }
    }
}
