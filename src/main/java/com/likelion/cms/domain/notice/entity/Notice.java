package com.likelion.cms.domain.notice.entity;

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
@Table(name = "Notice", indexes = {
        @Index(name = "idx_notice_image_asset", columnList = "imageAssetId"),
        @Index(name = "idx_notice_created_by", columnList = "createdBy"),
        @Index(name = "idx_notice_tag", columnList = "tag"),
        @Index(name = "idx_notice_published_at", columnList = "publishedAt"),
        @Index(name = "idx_notice_fixed_published", columnList = "isFixed, publishedAt, noticeId")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long noticeId;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NoticeTag tag;

    @Column(nullable = false)
    private Boolean isFixed;

    @Column(length = 2048)
    private String externalUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "imageAssetId")
    private FileAsset imageAsset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "createdBy", nullable = false)
    private AppUser createdByUser;

    @Column(nullable = false)
    private LocalDateTime publishedAt;

    @Version
    @Column(nullable = false)
    private Integer version;

    private LocalDateTime archivedAt;

    @Builder
    private Notice(String title, String content, NoticeTag tag, Boolean isFixed,
                   String externalUrl, FileAsset imageAsset, AppUser createdByUser,
                   LocalDateTime publishedAt) {
        this.title = title;
        this.content = content;
        this.tag = tag;
        this.isFixed = isFixed != null ? isFixed : false;
        this.externalUrl = externalUrl;
        this.imageAsset = imageAsset;
        this.createdByUser = createdByUser;
        this.publishedAt = publishedAt;
    }
}
