package com.likelion.cms.domain.project.entity;

import com.likelion.cms.domain.cohort.entity.Cohort;
import com.likelion.cms.domain.user.entity.AppUser;
import com.likelion.cms.global.entity.BaseTimeEntity;
import com.likelion.cms.support.file.entity.FileAsset;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "Project", indexes = {
        @Index(name = "idx_project_thumbnail", columnList = "thumbnailAssetId"),
        @Index(name = "idx_project_cohort_id", columnList = "cohortId"),
        @Index(name = "idx_project_created_by", columnList = "createdBy"),
        @Index(name = "idx_project_type", columnList = "projectType"),
        @Index(name = "idx_project_created_at", columnList = "createdAt"),
        @Index(name = "idx_project_cohort_created", columnList = "cohortId, createdAt")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deletedAt IS NULL")
public class Project extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long projectId;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(nullable = false, length = 50)
    private String projectType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thumbnailAssetId")
    private FileAsset thumbnailAsset;

    @Column(length = 2048)
    private String deployUrl;

    @Column(length = 2048)
    private String githubUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cohortId", nullable = false)
    private Cohort cohort;

    @Column(nullable = false)
    private LocalDate startedMonth;

    @Column(nullable = false)
    private LocalDate endedMonth;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "createdBy", nullable = false)
    private AppUser createdByUser;

    @Version
    @Column(nullable = false)
    private Integer version;

    private LocalDateTime deletedAt;

    @Builder
    private Project(String title, String description, String projectType,
                    FileAsset thumbnailAsset, String deployUrl, String githubUrl,
                    Cohort cohort, LocalDate startedMonth, LocalDate endedMonth,
                    AppUser createdByUser) {
        this.title = title;
        this.description = description;
        this.projectType = projectType;
        this.thumbnailAsset = thumbnailAsset;
        this.deployUrl = deployUrl;
        this.githubUrl = githubUrl;
        this.cohort = cohort;
        this.startedMonth = startedMonth;
        this.endedMonth = endedMonth;
        this.createdByUser = createdByUser;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void updateProjectType(String projectType) {
        this.projectType = projectType;
    }

    public void updateThumbnailAsset(FileAsset thumbnailAsset) {
        this.thumbnailAsset = thumbnailAsset;
    }

    public void updateDeployUrl(String deployUrl) {
        this.deployUrl = deployUrl;
    }

    public void updateGithubUrl(String githubUrl) {
        this.githubUrl = githubUrl;
    }

    public void updateCohort(Cohort cohort) {
        this.cohort = cohort;
    }

    public void updateStartedMonth(LocalDate startedMonth) {
        this.startedMonth = startedMonth;
    }

    public void updateEndedMonth(LocalDate endedMonth) {
        this.endedMonth = endedMonth;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
