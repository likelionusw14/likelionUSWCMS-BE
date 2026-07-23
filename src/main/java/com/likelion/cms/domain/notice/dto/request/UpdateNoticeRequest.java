package com.likelion.cms.domain.notice.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.likelion.cms.domain.notice.entity.NoticeTag;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateNoticeRequest {

    @NotNull
    @PositiveOrZero
    private Integer version;

    private String title;
    private String content;
    private NoticeTag tag;
    private Boolean isFixed;
    private String externalUrl;
    private Long imageAssetId;

    @JsonIgnore
    private boolean titleProvided;
    @JsonIgnore
    private boolean contentProvided;
    @JsonIgnore
    private boolean tagProvided;
    @JsonIgnore
    private boolean isFixedProvided;
    @JsonIgnore
    private boolean externalUrlProvided;
    @JsonIgnore
    private boolean imageAssetIdProvided;

    @JsonSetter
    public void setVersion(Integer version) {
        this.version = version;
    }

    @JsonSetter
    public void setTitle(String title) {
        this.titleProvided = true;
        this.title = title;
    }

    @JsonSetter
    public void setContent(String content) {
        this.contentProvided = true;
        this.content = content;
    }

    @JsonSetter
    public void setTag(NoticeTag tag) {
        this.tagProvided = true;
        this.tag = tag;
    }

    @JsonSetter
    public void setIsFixed(Boolean isFixed) {
        this.isFixedProvided = true;
        this.isFixed = isFixed;
    }

    @JsonSetter
    public void setExternalUrl(String externalUrl) {
        this.externalUrlProvided = true;
        this.externalUrl = externalUrl;
    }

    @JsonSetter
    public void setImageAssetId(Long imageAssetId) {
        this.imageAssetIdProvided = true;
        this.imageAssetId = imageAssetId;
    }

    @AssertTrue(message = "version 외에 하나 이상의 수정 필드가 필요합니다.")
    public boolean isAnyChangeProvided() {
        return titleProvided || contentProvided || tagProvided || isFixedProvided
                || externalUrlProvided || imageAssetIdProvided;
    }

    @AssertTrue(message = "수정 필드의 값이 올바르지 않습니다.")
    public boolean isProvidedValueValid() {
        boolean validTitle = !titleProvided
                || (title != null && !title.isBlank() && title.length() <= 150);
        boolean validContent = !contentProvided
                || (content != null && !content.isBlank() && content.length() <= 20000);
        boolean validTag = !tagProvided || tag != null;
        boolean validIsFixed = !isFixedProvided || isFixed != null;
        boolean validExternalUrl = !externalUrlProvided
                || (externalUrl == null || externalUrl.length() <= 2048);
        boolean validImageAssetId = !imageAssetIdProvided
                || imageAssetId == null || imageAssetId > 0;
        return validTitle && validContent && validTag && validIsFixed
                && validExternalUrl && validImageAssetId;
    }

    @AssertTrue(message = "externalUrl은 http 또는 https URL이어야 합니다.")
    public boolean isExternalUrlValid() {
        return !externalUrlProvided || CreateNoticeRequest.isHttpUrl(externalUrl);
    }
}
