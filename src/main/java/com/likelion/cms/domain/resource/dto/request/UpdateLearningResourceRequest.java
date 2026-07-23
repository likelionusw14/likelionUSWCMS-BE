package com.likelion.cms.domain.resource.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.likelion.cms.common.type.PartType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateLearningResourceRequest {

    @NotNull
    @PositiveOrZero
    private Integer version;

    private String title;
    private Integer week;
    private PartType targetPart;
    private Long fileAssetId;

    @JsonIgnore
    private boolean titleProvided;
    @JsonIgnore
    private boolean weekProvided;
    @JsonIgnore
    private boolean targetPartProvided;
    @JsonIgnore
    private boolean fileAssetIdProvided;

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
    public void setWeek(Integer week) {
        this.weekProvided = true;
        this.week = week;
    }

    @JsonSetter
    public void setTargetPart(PartType targetPart) {
        this.targetPartProvided = true;
        this.targetPart = targetPart;
    }

    @JsonSetter
    public void setFileAssetId(Long fileAssetId) {
        this.fileAssetIdProvided = true;
        this.fileAssetId = fileAssetId;
    }

    @AssertTrue(message = "version 외에 하나 이상의 수정 필드가 필요합니다.")
    public boolean isAnyChangeProvided() {
        return titleProvided || weekProvided || targetPartProvided || fileAssetIdProvided;
    }

    @AssertTrue(message = "수정 필드의 값이 올바르지 않습니다.")
    public boolean isProvidedValueValid() {
        boolean validTitle = !titleProvided
                || (title != null && !title.isBlank() && title.length() <= 150);
        boolean validWeek = !weekProvided || (week != null && week >= 1 && week <= 52);
        boolean validTargetPart = !targetPartProvided || targetPart != null;
        boolean validFileAssetId = !fileAssetIdProvided || (fileAssetId != null && fileAssetId > 0);
        return validTitle && validWeek && validTargetPart && validFileAssetId;
    }
}
