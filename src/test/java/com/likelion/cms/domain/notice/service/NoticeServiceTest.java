package com.likelion.cms.domain.notice.service;

import com.likelion.cms.domain.notice.dto.request.CreateNoticeRequest;
import com.likelion.cms.domain.notice.dto.request.UpdateNoticeRequest;
import com.likelion.cms.domain.notice.dto.response.NoticeResponse;
import com.likelion.cms.domain.notice.entity.Notice;
import com.likelion.cms.domain.notice.entity.NoticeTag;
import com.likelion.cms.domain.notice.repository.NoticeRepository;
import com.likelion.cms.domain.user.entity.AppUser;
import com.likelion.cms.domain.user.repository.AppUserRepository;
import com.likelion.cms.global.exception.BusinessException;
import com.likelion.cms.global.exception.ErrorCode;
import com.likelion.cms.support.file.entity.FileAsset;
import com.likelion.cms.support.file.entity.FilePurpose;
import com.likelion.cms.support.file.repository.FileAssetRepository;
import com.likelion.cms.global.response.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

    @Mock
    private NoticeRepository noticeRepository;

    @Mock
    private FileAssetRepository fileAssetRepository;

    @Mock
    private AppUserRepository appUserRepository;

    private NoticeService noticeService;

    @BeforeEach
    void setUp() {
        noticeService = new NoticeService(noticeRepository, fileAssetRepository, appUserRepository);
    }

    @Test
    void listReturnsPagedNoticesWithoutTagFilter() {
        AppUser actor = actor(1L);
        Notice fixedNotice = notice(actor, null);
        initializeEntity(fixedNotice, 1L, 0);
        Notice normalNotice = notice(actor, null);
        initializeEntity(normalNotice, 2L, 0);

        Pageable pageable = PageRequest.of(0, 20);
        Page<Notice> noticePage = new PageImpl<>(List.of(fixedNotice, normalNotice), pageable, 2);

        when(noticeRepository.findAllActiveByTag(isNull(), eq(pageable))).thenReturn(noticePage);

        PageResponse<NoticeResponse> response = noticeService.list(null, 0, 20);

        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems().get(0).getNoticeId()).isEqualTo(1L);
        assertThat(response.getPage().getTotalElements()).isEqualTo(2);
        assertThat(response.getPage().getTotalPages()).isEqualTo(1);
        assertThat(response.getPage().isHasNext()).isFalse();
    }

    @Test
    void listFiltersByTagWhenProvided() {
        AppUser actor = actor(1L);
        Notice notice = notice(actor, null);
        initializeEntity(notice, 3L, 0);

        Pageable pageable = PageRequest.of(0, 20);
        Page<Notice> noticePage = new PageImpl<>(List.of(notice), pageable, 1);

        when(noticeRepository.findAllActiveByTag(eq(NoticeTag.PROJECT), eq(pageable))).thenReturn(noticePage);

        PageResponse<NoticeResponse> response = noticeService.list(NoticeTag.PROJECT, 0, 20);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getTag()).isEqualTo(NoticeTag.PROJECT);
    }

    @Test
    void getReturnsNoticeWhenFound() {
        AppUser actor = actor(1L);
        Notice notice = notice(actor, null);
        initializeEntity(notice, 30L, 0);

        when(noticeRepository.findById(30L)).thenReturn(Optional.of(notice));

        NoticeResponse response = noticeService.get(30L);

        assertThat(response.getNoticeId()).isEqualTo(30L);
        assertThat(response.getTitle()).isEqualTo("기존 공지");
    }

    @Test
    void getRejectsMissingNotice() {
        when(noticeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noticeService.get(99L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void createSavesNoticeWithoutOptionalImage() {
        AppUser actor = actor(1L);
        CreateNoticeRequest request = new CreateNoticeRequest(
                " 새 공지 ",
                " 공지 내용 ",
                NoticeTag.SCHEDULE,
                null,
                "https://example.com/notices/1",
                null
        );

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(noticeRepository.save(any(Notice.class))).thenAnswer(invocation -> {
            Notice notice = invocation.getArgument(0);
            initializeEntity(notice, 30L, 0);
            return notice;
        });

        NoticeResponse response = noticeService.create(request, 1L);

        assertThat(response.getNoticeId()).isEqualTo(30L);
        assertThat(response.getTitle()).isEqualTo("새 공지");
        assertThat(response.getContent()).isEqualTo("공지 내용");
        assertThat(response.getIsFixed()).isFalse();
        assertThat(response.getPublishedAt()).isNotNull();
        verify(fileAssetRepository, never()).findById(any());
    }

    @Test
    void createRejectsFileForDifferentPurpose() {
        AppUser actor = actor(1L);
        FileAsset fileAsset = fileAsset(FilePurpose.LEARNING_RESOURCE);
        CreateNoticeRequest request = new CreateNoticeRequest(
                "공지", "내용", NoticeTag.OTHER, false, null, 10L
        );

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(fileAssetRepository.findById(10L)).thenReturn(Optional.of(fileAsset));

        assertThatThrownBy(() -> noticeService.create(request, 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        verify(noticeRepository, never()).save(any());
    }

    @Test
    void updateClearsNullableFieldsWhenExplicitNullIsProvided() {
        AppUser actor = actor(1L);
        Notice notice = notice(actor, fileAsset(FilePurpose.NOTICE_IMAGE));
        initializeEntity(notice, 30L, 2);
        UpdateNoticeRequest request = new UpdateNoticeRequest();
        request.setVersion(2);
        request.setExternalUrl(null);
        request.setImageAssetId(null);

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(noticeRepository.findById(30L)).thenReturn(Optional.of(notice));

        NoticeResponse response = noticeService.update(30L, request, 1L);

        assertThat(response.getExternalUrl()).isNull();
        assertThat(notice.getImageAsset()).isNull();
        verify(fileAssetRepository, never()).findById(any());
    }

    @Test
    void updateKeepsFieldsThatWereNotProvided() {
        AppUser actor = actor(1L);
        Notice notice = notice(actor, null);
        initializeEntity(notice, 30L, 2);
        UpdateNoticeRequest request = new UpdateNoticeRequest();
        request.setVersion(2);
        request.setTitle(" 수정 공지 ");

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(noticeRepository.findById(30L)).thenReturn(Optional.of(notice));

        NoticeResponse response = noticeService.update(30L, request, 1L);

        assertThat(response.getTitle()).isEqualTo("수정 공지");
        assertThat(response.getContent()).isEqualTo("기존 내용");
        assertThat(response.getIsFixed()).isTrue();
    }

    @Test
    void updateRejectsVersionConflict() {
        AppUser actor = actor(1L);
        Notice notice = notice(actor, null);
        initializeEntity(notice, 30L, 2);
        UpdateNoticeRequest request = new UpdateNoticeRequest();
        request.setVersion(1);
        request.setTitle("수정 공지");

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(noticeRepository.findById(30L)).thenReturn(Optional.of(notice));

        assertThatThrownBy(() -> noticeService.update(30L, request, 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.OPTIMISTIC_LOCK_CONFLICT));
    }

    @Test
    void createRejectsMissingImageAsset() {
        AppUser actor = actor(1L);
        CreateNoticeRequest request = new CreateNoticeRequest(
                "공지", "내용", NoticeTag.OTHER, false, null, 10L
        );

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(fileAssetRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noticeService.create(request, 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
        verify(noticeRepository, never()).save(any());
    }

    @Test
    void updateRejectsMissingNotice() {
        AppUser actor = actor(1L);
        UpdateNoticeRequest request = new UpdateNoticeRequest();
        request.setVersion(0);
        request.setTitle("수정 공지");

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(noticeRepository.findById(30L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noticeService.update(30L, request, 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private AppUser actor(Long userId) {
        AppUser actor = mock(AppUser.class);
        lenient().when(actor.getUserId()).thenReturn(userId);
        return actor;
    }

    private FileAsset fileAsset(FilePurpose purpose) {
        FileAsset fileAsset = mock(FileAsset.class);
        lenient().when(fileAsset.getPurpose()).thenReturn(purpose);
        return fileAsset;
    }

    private Notice notice(AppUser actor, FileAsset imageAsset) {
        return Notice.builder()
                .title("기존 공지")
                .content("기존 내용")
                .tag(NoticeTag.PROJECT)
                .isFixed(true)
                .externalUrl("https://example.com/old")
                .imageAsset(imageAsset)
                .createdByUser(actor)
                .publishedAt(LocalDateTime.of(2026, 7, 20, 10, 0))
                .build();
    }

    private void initializeEntity(Notice notice, Long noticeId, Integer version) {
        ReflectionTestUtils.setField(notice, "noticeId", noticeId);
        ReflectionTestUtils.setField(notice, "version", version);
        ReflectionTestUtils.setField(notice, "createdAt", LocalDateTime.of(2026, 7, 20, 10, 0));
        ReflectionTestUtils.setField(notice, "updatedAt", LocalDateTime.of(2026, 7, 20, 10, 0));
    }
}