package com.likelion.cms.support.file.service;

import com.likelion.cms.global.exception.BusinessException;
import com.likelion.cms.global.exception.ErrorCode;
import com.likelion.cms.support.file.dto.request.FileAssetRequest;
import com.likelion.cms.support.file.dto.request.FileUploadUrlRequest;
import com.likelion.cms.support.file.dto.response.FileAssetResponse;
import com.likelion.cms.support.file.dto.response.FileUploadUrlResponse;
import com.likelion.cms.support.file.entity.FilePurpose;
import com.likelion.cms.support.file.storage.FileStorage;
import com.likelion.cms.support.file.store.FileIdempotencyRecord;
import com.likelion.cms.support.file.store.FileIdempotencyStore;
import com.likelion.cms.support.file.store.FileUploadGrant;
import com.likelion.cms.support.file.store.FileUploadGrantStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileAssetServiceTest {

    private static final Long ACTOR_ID = 7L;
    private static final UUID IDEMPOTENCY_KEY =
            UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final String OBJECT_KEY =
            "learning-resource/2026/07/25/123e4567-e89b-12d3-a456-426614174000.pdf";
    private static final String CHECKSUM_HEX = "00".repeat(32);
    private static final String CHECKSUM_BASE64 =
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    @Mock
    private FileObjectKeyFactory objectKeyFactory;
    @Mock
    private FileStorage fileStorage;
    @Mock
    private FileUploadGrantStore uploadGrantStore;
    @Mock
    private FileIdempotencyStore idempotencyStore;
    @Mock
    private FileAssetRegistrationService registrationService;

    private FileAssetService service;

    @BeforeEach
    void setUp() {
        service = new FileAssetService(
                new FileUploadPolicy(),
                objectKeyFactory,
                fileStorage,
                uploadGrantStore,
                idempotencyStore,
                registrationService
        );
    }

    @Test
    void createsPresignedUploadUrlAndStoresGrant() {
        OffsetDateTime expiresAt = OffsetDateTime.of(
                2026, 7, 25, 12, 10, 0, 0, ZoneOffset.UTC
        );
        when(objectKeyFactory.create(FilePurpose.LEARNING_RESOURCE, "pdf"))
                .thenReturn(OBJECT_KEY);
        when(fileStorage.createUploadUrl(OBJECT_KEY, "application/pdf", CHECKSUM_BASE64))
                .thenReturn(new FileStorage.PresignedUpload(
                        "https://example.s3.amazonaws.com/upload",
                        Map.of("content-type", "application/pdf"),
                        expiresAt
                ));

        FileUploadUrlResponse response = service.createUploadUrl(
                new FileUploadUrlRequest(
                        FilePurpose.LEARNING_RESOURCE,
                        "lecture.pdf",
                        "application/pdf",
                        1024L,
                        CHECKSUM_HEX
                ),
                ACTOR_ID
        );

        assertThat(response.getObjectKey()).isEqualTo(OBJECT_KEY);
        assertThat(response.getExpiresAt()).isEqualTo(expiresAt);
        ArgumentCaptor<FileUploadGrant> grantCaptor =
                ArgumentCaptor.forClass(FileUploadGrant.class);
        verify(uploadGrantStore).save(grantCaptor.capture());
        assertThat(grantCaptor.getValue().actorUserId()).isEqualTo(ACTOR_ID);
        assertThat(grantCaptor.getValue().checksumSha256()).isEqualTo(CHECKSUM_HEX);
    }

    @Test
    void completesUploadAfterGrantAndS3MetadataMatch() {
        FileAssetRequest request = fileAssetRequest();
        FileUploadGrant grant = uploadGrant();
        FileAssetResponse registered = fileAssetResponse();
        when(idempotencyStore.find(ACTOR_ID, IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(idempotencyStore.reserve(ACTOR_ID, IDEMPOTENCY_KEY, OBJECT_KEY)).thenReturn(true);
        when(uploadGrantStore.find(OBJECT_KEY)).thenReturn(Optional.of(grant));
        when(fileStorage.findMetadata(OBJECT_KEY)).thenReturn(Optional.of(
                new FileStorage.StoredFileMetadata(
                        "application/pdf",
                        1024L,
                        CHECKSUM_BASE64
                )
        ));
        when(registrationService.register(grant)).thenReturn(registered);

        FileAssetResponse response = service.completeUpload(
                request,
                ACTOR_ID,
                IDEMPOTENCY_KEY
        );

        assertThat(response.getFileAssetId()).isEqualTo(10L);
        verify(idempotencyStore).complete(ACTOR_ID, IDEMPOTENCY_KEY, OBJECT_KEY, 10L);
        verify(uploadGrantStore).delete(OBJECT_KEY);
        verify(idempotencyStore, never()).clearPending(any(), any(), any());
    }

    @Test
    void returnsCompletedIdempotentResultWithoutReadingS3Again() {
        FileAssetResponse registered = fileAssetResponse();
        when(idempotencyStore.find(ACTOR_ID, IDEMPOTENCY_KEY)).thenReturn(Optional.of(
                FileIdempotencyRecord.completed(OBJECT_KEY, 10L)
        ));
        when(registrationService.findOwned(10L, ACTOR_ID, OBJECT_KEY))
                .thenReturn(registered);

        FileAssetResponse response = service.completeUpload(
                fileAssetRequest(),
                ACTOR_ID,
                IDEMPOTENCY_KEY
        );

        assertThat(response).isSameAs(registered);
        verifyNoInteractions(fileStorage);
        verify(uploadGrantStore, never()).find(any());
    }

    @Test
    void rejectsReusedIdempotencyKeyForDifferentObject() {
        when(idempotencyStore.find(ACTOR_ID, IDEMPOTENCY_KEY)).thenReturn(Optional.of(
                FileIdempotencyRecord.completed("different-key.pdf", 10L)
        ));

        assertThatThrownBy(() -> service.completeUpload(
                fileAssetRequest(),
                ACTOR_ID,
                IDEMPOTENCY_KEY
        )).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.FILE_IDEMPOTENCY_CONFLICT));
    }

    @Test
    void rejectsRequestThatDoesNotMatchUploadGrantAndClearsReservation() {
        FileUploadGrant differentGrant = new FileUploadGrant(
                ACTOR_ID,
                FilePurpose.LEARNING_RESOURCE,
                OBJECT_KEY,
                "different.pdf",
                "application/pdf",
                1024L,
                CHECKSUM_HEX
        );
        when(idempotencyStore.find(ACTOR_ID, IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(idempotencyStore.reserve(ACTOR_ID, IDEMPOTENCY_KEY, OBJECT_KEY)).thenReturn(true);
        when(uploadGrantStore.find(OBJECT_KEY)).thenReturn(Optional.of(differentGrant));

        assertThatThrownBy(() -> service.completeUpload(
                fileAssetRequest(),
                ACTOR_ID,
                IDEMPOTENCY_KEY
        )).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.FILE_METADATA_MISMATCH));

        verify(idempotencyStore).clearPending(ACTOR_ID, IDEMPOTENCY_KEY, OBJECT_KEY);
        verifyNoInteractions(fileStorage);
    }

    @Test
    void rejectsMissingS3ObjectAndClearsReservation() {
        when(idempotencyStore.find(ACTOR_ID, IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(idempotencyStore.reserve(ACTOR_ID, IDEMPOTENCY_KEY, OBJECT_KEY)).thenReturn(true);
        when(uploadGrantStore.find(OBJECT_KEY)).thenReturn(Optional.of(uploadGrant()));
        when(fileStorage.findMetadata(OBJECT_KEY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.completeUpload(
                fileAssetRequest(),
                ACTOR_ID,
                IDEMPOTENCY_KEY
        )).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.FILE_UPLOAD_NOT_FOUND));

        verify(idempotencyStore).clearPending(ACTOR_ID, IDEMPOTENCY_KEY, OBJECT_KEY);
    }

    @Test
    void rejectsS3MetadataMismatch() {
        when(idempotencyStore.find(ACTOR_ID, IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(idempotencyStore.reserve(ACTOR_ID, IDEMPOTENCY_KEY, OBJECT_KEY)).thenReturn(true);
        when(uploadGrantStore.find(OBJECT_KEY)).thenReturn(Optional.of(uploadGrant()));
        when(fileStorage.findMetadata(OBJECT_KEY)).thenReturn(Optional.of(
                new FileStorage.StoredFileMetadata(
                        "application/pdf",
                        2048L,
                        CHECKSUM_BASE64
                )
        ));

        assertThatThrownBy(() -> service.completeUpload(
                fileAssetRequest(),
                ACTOR_ID,
                IDEMPOTENCY_KEY
        )).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.FILE_METADATA_MISMATCH));

        verify(registrationService, never()).register(any());
        verify(idempotencyStore).clearPending(ACTOR_ID, IDEMPOTENCY_KEY, OBJECT_KEY);
    }

    private FileAssetRequest fileAssetRequest() {
        return new FileAssetRequest(
                FilePurpose.LEARNING_RESOURCE,
                OBJECT_KEY,
                "lecture.pdf",
                "application/pdf",
                1024L,
                CHECKSUM_HEX
        );
    }

    private FileUploadGrant uploadGrant() {
        return new FileUploadGrant(
                ACTOR_ID,
                FilePurpose.LEARNING_RESOURCE,
                OBJECT_KEY,
                "lecture.pdf",
                "application/pdf",
                1024L,
                CHECKSUM_HEX
        );
    }

    private FileAssetResponse fileAssetResponse() {
        return FileAssetResponse.of(
                10L,
                FilePurpose.LEARNING_RESOURCE,
                "lecture.pdf",
                "application/pdf",
                1024L,
                LocalDateTime.of(2026, 7, 25, 21, 0)
        );
    }
}
