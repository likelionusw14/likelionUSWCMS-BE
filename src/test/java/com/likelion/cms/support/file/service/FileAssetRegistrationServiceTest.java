package com.likelion.cms.support.file.service;

import com.likelion.cms.domain.user.entity.AppUser;
import com.likelion.cms.domain.user.repository.AppUserRepository;
import com.likelion.cms.global.exception.BusinessException;
import com.likelion.cms.global.exception.ErrorCode;
import com.likelion.cms.support.file.dto.response.FileAssetResponse;
import com.likelion.cms.support.file.entity.FileAsset;
import com.likelion.cms.support.file.entity.FilePurpose;
import com.likelion.cms.support.file.repository.FileAssetRepository;
import com.likelion.cms.support.file.store.FileUploadGrant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileAssetRegistrationServiceTest {

    @Mock
    private FileAssetRepository fileAssetRepository;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private AppUser actor;

    private FileAssetRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new FileAssetRegistrationService(fileAssetRepository, appUserRepository);
    }

    @Test
    void registersValidatedGrantAsFileAsset() {
        FileUploadGrant grant = grant();
        when(fileAssetRepository.findByObjectKey(grant.objectKey())).thenReturn(Optional.empty());
        when(appUserRepository.findById(7L)).thenReturn(Optional.of(actor));
        when(fileAssetRepository.saveAndFlush(any(FileAsset.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FileAssetResponse response = service.register(grant);

        ArgumentCaptor<FileAsset> assetCaptor = ArgumentCaptor.forClass(FileAsset.class);
        verify(fileAssetRepository).saveAndFlush(assetCaptor.capture());
        FileAsset saved = assetCaptor.getValue();
        assertThat(saved.getPurpose()).isEqualTo(FilePurpose.LEARNING_RESOURCE);
        assertThat(saved.getObjectKey()).isEqualTo(grant.objectKey());
        assertThat(saved.getUploadedByUser()).isSameAs(actor);
        assertThat(response.getMimeType()).isEqualTo("application/pdf");
    }

    @Test
    void rejectsIdempotentResultOwnedByAnotherUser() {
        FileAsset asset = org.mockito.Mockito.mock(FileAsset.class);
        AppUser differentActor = org.mockito.Mockito.mock(AppUser.class);
        when(fileAssetRepository.findById(10L)).thenReturn(Optional.of(asset));
        when(asset.getUploadedByUser()).thenReturn(differentActor);
        when(differentActor.getUserId()).thenReturn(8L);

        assertThatThrownBy(() -> service.findOwned(10L, 7L, grant().objectKey()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.FILE_IDEMPOTENCY_CONFLICT));
    }

    private FileUploadGrant grant() {
        return new FileUploadGrant(
                7L,
                FilePurpose.LEARNING_RESOURCE,
                "learning-resource/2026/07/25/id.pdf",
                "lecture.pdf",
                "application/pdf",
                1024L,
                null
        );
    }
}
