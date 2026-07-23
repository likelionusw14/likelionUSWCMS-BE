package com.likelion.cms.domain.notice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.likelion.cms.domain.notice.entity.Notice;
import com.likelion.cms.domain.notice.entity.NoticeTag;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record NoticeResponse(
        Long noticeId,
        String title,
        String content,
        NoticeTag tag,
        Boolean isFixed,
        String externalUrl,
        Long createdBy,
        LocalDateTime publishedAt,
        Integer version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static NoticeResponse from(Notice notice) {
        return new NoticeResponse(
                notice.getNoticeId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getTag(),
                notice.getIsFixed(),
                notice.getExternalUrl(),
                notice.getCreatedByUser().getUserId(),
                notice.getPublishedAt(),
                notice.getVersion(),
                notice.getCreatedAt(),
                notice.getUpdatedAt()
        );
    }
}
