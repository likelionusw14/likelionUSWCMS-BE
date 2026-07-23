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
import com.likelion.cms.support.file.entity.FileAsset;
import com.likelion.cms.support.file.entity.FilePurpose;
import com.likelion.cms.support.file.repository.FileAssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Mock
    private LearningResourceRepository learningResourceRepository;

    @Mock
    private FileAssetRepository fileAssetRepository;

    @Mock
    private AppUserRepository appUserRepository;

    private ResourceService resourceService;

    @BeforeEach
    void setUp() {
        resourceService = new ResourceService(
                learningResourceRepository,
                fileAssetRepository,
                appUserRepository
        );
    }

    @Test
    void createSavesLearningResourceWithValidatedFile() {
        AppUser actor = actor(1L);
        FileAsset fileAsset = fileAsset(10L, FilePurpose.LEARNING_RESOURCE);
        CreateLearningResourceRequest request = new CreateLearningResourceRequest(
                " 1주차 자료 ", 1, PartType.BACKEND, 10L
        );

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(fileAssetRepository.findById(10L)).thenReturn(Optional.of(fileAsset));
        when(learningResourceRepository.save(any(LearningResource.class))).thenAnswer(invocation -> {
            LearningResource resource = invocation.getArgument(0);
            initializeEntity(resource, 20L, 0);
            return resource;
        });

        LearningResourceResponse response = resourceService.create(request, 1L);

        assertThat(response.resourceId()).isEqualTo(20L);
        assertThat(response.title()).isEqualTo("1주차 자료");
        assertThat(response.file().fileAssetId()).isEqualTo(10L);
        assertThat(response.createdBy()).isEqualTo(1L);
    }

    @Test
    void updateChangesOnlyProvidedFields() {
        AppUser actor = actor(1L);
        LearningResource resource = resource(actor, fileAsset(10L, FilePurpose.LEARNING_RESOURCE));
        initializeEntity(resource, 20L, 3);
        UpdateLearningResourceRequest request = new UpdateLearningResourceRequest();
        request.setVersion(3);
        request.setTitle(" 수정된 제목 ");

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(learningResourceRepository.findById(20L)).thenReturn(Optional.of(resource));

        LearningResourceResponse response = resourceService.update(20L, request, 1L);

        assertThat(response.title()).isEqualTo("수정된 제목");
        assertThat(response.week()).isEqualTo(1);
        verify(fileAssetRepository, never()).findById(any());
    }

    @Test
    void updateRejectsVersionConflict() {
        AppUser actor = actor(1L);
        LearningResource resource = resource(actor, fileAsset(10L, FilePurpose.LEARNING_RESOURCE));
        initializeEntity(resource, 20L, 3);
        UpdateLearningResourceRequest request = new UpdateLearningResourceRequest();
        request.setVersion(2);
        request.setTitle("수정된 제목");

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(learningResourceRepository.findById(20L)).thenReturn(Optional.of(resource));

        assertThatThrownBy(() -> resourceService.update(20L, request, 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.OPTIMISTIC_LOCK_CONFLICT));
    }

    @Test
    void createRejectsFileForDifferentPurpose() {
        AppUser actor = actor(1L);
        FileAsset fileAsset = fileAsset(10L, FilePurpose.NOTICE_IMAGE);
        CreateLearningResourceRequest request = new CreateLearningResourceRequest(
                "1주차 자료", 1, PartType.BACKEND, 10L
        );

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(fileAssetRepository.findById(10L)).thenReturn(Optional.of(fileAsset));

        assertThatThrownBy(() -> resourceService.create(request, 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        verify(learningResourceRepository, never()).save(any());
    }

    @Test
    void createRejectsMissingFileAsset() {
        AppUser actor = actor(1L);
        CreateLearningResourceRequest request = new CreateLearningResourceRequest(
                "1주차 자료", 1, PartType.BACKEND, 10L
        );

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(fileAssetRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceService.create(request, 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
        verify(learningResourceRepository, never()).save(any());
    }

    @Test
    void updateRejectsMissingLearningResource() {
        AppUser actor = actor(1L);
        UpdateLearningResourceRequest request = new UpdateLearningResourceRequest();
        request.setVersion(0);
        request.setTitle("수정된 제목");

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(learningResourceRepository.findById(20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceService.update(20L, request, 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private AppUser actor(Long userId) {
        AppUser actor = mock(AppUser.class);
        lenient().when(actor.getUserId()).thenReturn(userId);
        return actor;
    }

    private FileAsset fileAsset(Long fileAssetId, FilePurpose purpose) {
        FileAsset fileAsset = mock(FileAsset.class);
        lenient().when(fileAsset.getFileAssetId()).thenReturn(fileAssetId);
        lenient().when(fileAsset.getPurpose()).thenReturn(purpose);
        lenient().when(fileAsset.getOriginalFileName()).thenReturn("resource.pdf");
        lenient().when(fileAsset.getMimeType()).thenReturn("application/pdf");
        lenient().when(fileAsset.getSizeBytes()).thenReturn(1024L);
        lenient().when(fileAsset.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 7, 20, 10, 0));
        return fileAsset;
    }

    private LearningResource resource(AppUser actor, FileAsset fileAsset) {
        return LearningResource.builder()
                .title("기존 제목")
                .week(1)
                .targetPart(PartType.BACKEND)
                .fileAsset(fileAsset)
                .createdByUser(actor)
                .build();
    }

    private void initializeEntity(LearningResource resource, Long resourceId, Integer version) {
        ReflectionTestUtils.setField(resource, "resourceId", resourceId);
        ReflectionTestUtils.setField(resource, "version", version);
        ReflectionTestUtils.setField(resource, "createdAt", LocalDateTime.of(2026, 7, 20, 10, 0));
        ReflectionTestUtils.setField(resource, "updatedAt", LocalDateTime.of(2026, 7, 20, 10, 0));
    }
}
