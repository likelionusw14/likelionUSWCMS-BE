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
import com.likelion.cms.global.response.PageMeta;
import com.likelion.cms.global.response.PageResponse;
import com.likelion.cms.support.file.entity.FileAsset;
import com.likelion.cms.support.file.entity.FilePurpose;
import com.likelion.cms.support.file.repository.FileAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final FileAssetRepository fileAssetRepository;
    private final AppUserRepository appUserRepository;

    public PageResponse<NoticeResponse> list(NoticeTag tag, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notice> noticePage = noticeRepository.findAllActiveByTag(tag, pageable);

        List<NoticeResponse> items = noticePage.getContent().stream()
                .map(NoticeResponse::from)
                .toList();

        PageMeta pageMeta = PageMeta.of(
                noticePage.getNumber(),
                noticePage.getSize(),
                noticePage.getTotalElements(),
                noticePage.getTotalPages(),
                noticePage.hasNext()
        );

        return PageResponse.of(items, pageMeta);
    }

    public NoticeResponse get(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .filter(found -> found.getArchivedAt() == null)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        return NoticeResponse.from(notice);
    }

    @Transactional
    public NoticeResponse create(CreateNoticeRequest request, Long actorUserId) {
        AppUser actor = findActor(actorUserId);
        FileAsset imageAsset = request.imageAssetId() == null
                ? null
                : findNoticeImage(request.imageAssetId());

        Notice notice = Notice.builder()
                .title(request.title().trim())
                .content(request.content().trim())
                .tag(request.tag())
                .isFixed(request.isFixed())
                .externalUrl(request.externalUrl())
                .imageAsset(imageAsset)
                .createdByUser(actor)
                .publishedAt(LocalDateTime.now())
                .build();

        return NoticeResponse.from(noticeRepository.save(notice));
    }

    @Transactional
    public NoticeResponse update(Long noticeId, UpdateNoticeRequest request, Long actorUserId) {
        findActor(actorUserId);
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        validateVersion(notice.getVersion(), request.getVersion());

        if (request.isTitleProvided()) {
            notice.updateTitle(request.getTitle().trim());
        }
        if (request.isContentProvided()) {
            notice.updateContent(request.getContent().trim());
        }
        if (request.isTagProvided()) {
            notice.updateTag(request.getTag());
        }
        if (request.isFixedProvided()) {
            notice.updateIsFixed(request.getIsFixed());
        }
        if (request.isExternalUrlProvided()) {
            notice.updateExternalUrl(request.getExternalUrl());
        }
        if (request.isImageAssetIdProvided()) {
            FileAsset imageAsset = request.getImageAssetId() == null
                    ? null
                    : findNoticeImage(request.getImageAssetId());
            notice.updateImageAsset(imageAsset);
        }

        return NoticeResponse.from(notice);
    }

    private AppUser findActor(Long actorUserId) {
        return appUserRepository.findById(actorUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    private FileAsset findNoticeImage(Long fileAssetId) {
        FileAsset fileAsset = fileAssetRepository.findById(fileAssetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (fileAsset.getPurpose() != FilePurpose.NOTICE_IMAGE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "공지 이미지 용도의 파일만 사용할 수 있습니다.");
        }
        return fileAsset;
    }

    private void validateVersion(Integer actualVersion, Integer requestedVersion) {
        if (!requestedVersion.equals(actualVersion)) {
            throw new BusinessException(ErrorCode.OPTIMISTIC_LOCK_CONFLICT);
        }
    }
}