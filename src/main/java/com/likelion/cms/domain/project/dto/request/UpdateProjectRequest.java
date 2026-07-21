package com.likelion.cms.domain.project.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class UpdateProjectRequest {

    @NotNull
    @PositiveOrZero
    private Integer version;

    private String title;
    private String description;
    private String projectType;
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
    public void setProjectType(String projectType) {
        this.projectTypeProvided = true;
        this.projectType = projectType;
    }

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
        boolean validProjectType = !projectTypeProvided
                || (projectType != null && !projectType.isBlank() && projectType.length() <= 50);
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
