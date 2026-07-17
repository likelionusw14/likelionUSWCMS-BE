package com.likelion.cms.domain.resource.entity;

import com.likelion.cms.common.type.PartType;
import com.likelion.cms.domain.user.entity.AppUser;
import com.likelion.cms.global.entity.BaseTimeEntity;
import com.likelion.cms.support.file.entity.FileAsset;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "LearningResource", indexes = {
        @Index(name = "idx_resource_file_asset", columnList = "fileAssetId"),
        @Index(name = "idx_resource_created_by", columnList = "createdBy"),
        @Index(name = "idx_resource_created_at", columnList = "createdAt"),
        @Index(name = "idx_resource_week", columnList = "week"),
        @Index(name = "idx_resource_target_part", columnList = "targetPart"),
        @Index(name = "idx_resource_week_part_created", columnList = "week, targetPart, createdAt")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LearningResource extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long resourceId;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false)
    private Integer week;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartType targetPart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fileAssetId", nullable = false)
    private FileAsset fileAsset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "createdBy", nullable = false)
    private AppUser createdByUser;

    @Version
    @Column(nullable = false)
    private Integer version;

    private LocalDateTime archivedAt;

    @Builder
    private LearningResource(String title, Integer week, PartType targetPart,
                              FileAsset fileAsset, AppUser createdByUser) {
        this.title = title;
        this.week = week;
        this.targetPart = targetPart;
        this.fileAsset = fileAsset;
        this.createdByUser = createdByUser;
    }
}
