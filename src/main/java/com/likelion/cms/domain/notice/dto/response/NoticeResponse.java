package com.likelion.cms.domain.notice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.likelion.cms.domain.notice.entity.Notice;
import com.likelion.cms.domain.notice.entity.NoticeTag;
import com.likelion.cms.support.file.dto.response.FileAssetResponse;
import com.likelion.cms.support.file.dto.response.FileView;
import com.likelion.cms.support.file.entity.FileAsset;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NoticeResponse {

    private final Long noticeId;
    private final String title;
    private final String content;
    private final NoticeTag tag;
    private final Boolean isFixed;
    private final String externalUrl;
    private final FileView image;
    private final Long createdBy;
    private final LocalDateTime publishedAt;

    /**
     * version : 낙관적 락(optimistic locking)을 위한 버전 값입니다.
     * 수정 요청 시 클라이언트가 조회 시점의 이 값을 그대로 전달해야 하며,
     * 서버에 저장된 현재 버전과 다르면 충돌로 간주해 요청이 거부됩니다.
     */
    private final Integer version;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static NoticeResponse of(Long noticeId, String title, String content, NoticeTag tag,
                                    Boolean isFixed, String externalUrl, FileView image, Long createdBy,
                                    LocalDateTime publishedAt, Integer version,
                                    LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new NoticeResponse(noticeId, title, content, tag, isFixed, externalUrl, image, createdBy,
                publishedAt, version, createdAt, updatedAt);
    }

    public static NoticeResponse from(Notice notice) {
        return of(notice.getNoticeId(), notice.getTitle(), notice.getContent(), notice.getTag(),
                notice.getIsFixed(), notice.getExternalUrl(), toFileView(notice.getImageAsset()),
                notice.getCreatedByUser().getUserId(), notice.getPublishedAt(), notice.getVersion(),
                notice.getCreatedAt(), notice.getUpdatedAt());
    }

    // downloadUrl/expiresAt은 null로 둔다. 목록/상세 응답에서 매번 S3 presign을 호출하지
    // 않는 ProjectResponse.thumbnail과 같은 관례를 따른 것으로, 실제 다운로드가 필요하면
    // 별도의 presigned URL 발급 엔드포인트를 쓰는 구조다.
    private static FileView toFileView(FileAsset imageAsset) {
        if (imageAsset == null) {
            return null;
        }
        return FileView.of(FileAssetResponse.from(imageAsset), null, null);
    }
}
