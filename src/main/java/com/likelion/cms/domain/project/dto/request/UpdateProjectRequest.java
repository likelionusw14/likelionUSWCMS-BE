package com.likelion.cms.domain.project.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.likelion.cms.common.type.ProjectType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// PATCH 부분 수정 요청. UpdateAccountRequest와 동일한 xxxProvided 패턴 -
// "필드를 안 보냄(유지)"과 "null로 보냄(제거)"을 구분하기 위해 @JsonSetter를 직접 씀.
// projectType은 CreateProjectRequest와 같은 이유로 String이 아니라 ProjectType enum.
@Getter
@NoArgsConstructor
public class UpdateProjectRequest {

    @NotNull
    @PositiveOrZero
    private Integer version;

    private String title;
    private String description;
    private ProjectType projectType;
    private Long thumbnailAssetId;
    private String deployUrl;
    private String githubUrl;
    private Long cohortId;
    private LocalDate startedMonth;
    private LocalDate endedMonth;

    @JsonIgnore
    private boolean titleProvided;
    @JsonIgnore
    private boolean descriptionProvided;
    @JsonIgnore
    private boolean projectTypeProvided;
    @JsonIgnore
    private boolean thumbnailAssetIdProvided;
    @JsonIgnore
    private boolean deployUrlProvided;
    @JsonIgnore
    private boolean githubUrlProvided;
    @JsonIgnore
    private boolean cohortIdProvided;
    @JsonIgnore
    private boolean startedMonthProvided;
    @JsonIgnore
    private boolean endedMonthProvided;

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
    public void setDescription(String description) {
        this.descriptionProvided = true;
        this.description = description;
    }

    @JsonSetter
    public void setProjectType(ProjectType projectType) {
        this.projectTypeProvided = true;
        this.projectType = projectType;
    }

    // 핵심 포인트: null도 "유효한 값"(썸네일 제거)이라, null이 와도 이 setter는
    // 호출되고 thumbnailAssetIdProvided는 true가 됨. "제공 여부"는 이 플래그로,
    // "제거인지 교체인지"는 null 여부로 서비스 레이어에서 나눠서 판단.
    @JsonSetter
    public void setThumbnailAssetId(Long thumbnailAssetId) {
        this.thumbnailAssetIdProvided = true;
        this.thumbnailAssetId = thumbnailAssetId;
    }

    @JsonSetter
    public void setDeployUrl(String deployUrl) {
        this.deployUrlProvided = true;
        this.deployUrl = deployUrl;
    }

    @JsonSetter
    public void setGithubUrl(String githubUrl) {
        this.githubUrlProvided = true;
        this.githubUrl = githubUrl;
    }

    @JsonSetter
    public void setCohortId(Long cohortId) {
        this.cohortIdProvided = true;
        this.cohortId = cohortId;
    }

    @JsonSetter
    public void setStartedMonth(LocalDate startedMonth) {
        this.startedMonthProvided = true;
        this.startedMonth = startedMonth;
    }

    @JsonSetter
    public void setEndedMonth(LocalDate endedMonth) {
        this.endedMonthProvided = true;
        this.endedMonth = endedMonth;
    }

    @AssertTrue(message = "version 외에 하나 이상의 수정 필드가 필요합니다.")
    public boolean isAnyChangeProvided() {
        return titleProvided || descriptionProvided || projectTypeProvided || thumbnailAssetIdProvided
                || deployUrlProvided || githubUrlProvided || cohortIdProvided
                || startedMonthProvided || endedMonthProvided;
    }

    @AssertTrue(message = "수정 필드의 값이 올바르지 않습니다.")
    public boolean isProvidedValueValid() {
        boolean validTitle = !titleProvided
                || (title != null && !title.isBlank() && title.length() <= 150);
        boolean validDescription = !descriptionProvided
                || (description != null && !description.isBlank() && description.length() <= 20000);
        boolean validProjectType = !projectTypeProvided || projectType != null;
        boolean validThumbnailAssetId = !thumbnailAssetIdProvided
                || thumbnailAssetId == null || thumbnailAssetId > 0;
        boolean validDeployUrl = !deployUrlProvided
                || (deployUrl == null || deployUrl.length() <= 2048);
        boolean validGithubUrl = !githubUrlProvided
                || (githubUrl == null || githubUrl.length() <= 2048);
        boolean validCohortId = !cohortIdProvided
                || (cohortId != null && cohortId > 0);
        boolean validStartedMonth = !startedMonthProvided || startedMonth != null;
        boolean validEndedMonth = !endedMonthProvided || endedMonth != null;
        return validTitle && validDescription && validProjectType && validThumbnailAssetId
                && validDeployUrl && validGithubUrl && validCohortId && validStartedMonth && validEndedMonth;
    }

    @AssertTrue(message = "deployUrl은 http 또는 https URL이어야 합니다.")
    public boolean isDeployUrlValid() {
        return !deployUrlProvided || CreateProjectRequest.isHttpUrl(deployUrl);
    }

    @AssertTrue(message = "githubUrl은 http 또는 https URL이어야 합니다.")
    public boolean isGithubUrlValid() {
        return !githubUrlProvided || CreateProjectRequest.isHttpUrl(githubUrl);
    }
}
