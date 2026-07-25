package com.likelion.cms.support.file.entity;

import com.likelion.cms.domain.user.entity.AppUser;
import com.likelion.cms.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "FileAsset", indexes = {
        @Index(name = "idx_file_asset_uploaded_by", columnList = "uploadedBy"),
        @Index(name = "idx_file_asset_purpose", columnList = "purpose"),
        @Index(name = "idx_file_asset_created_at", columnList = "createdAt")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deletedAt IS NULL")
public class FileAsset extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fileAssetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FilePurpose purpose;

    /**
     * objectKey : S3 오브젝트 키.
     * <p>unique 인덱스를 걸기 위해 768자로 제한한다. utf8mb4 는 문자당 4바이트라
     * InnoDB 인덱스 상한(3072바이트) 안에 들어가는 최대 길이가 768자다.</p>
     */
    @Column(nullable = false, unique = true, length = 768)
    private String objectKey;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false, length = 150)
    private String mimeType;

    @Column(nullable = false)
    private Long sizeBytes;

    @Column(length = 64, columnDefinition = "char(64)")
    private String checksumSha256;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploadedBy", nullable = false)
    private AppUser uploadedByUser;

    private LocalDateTime deletedAt;

    @Builder
    private FileAsset(FilePurpose purpose, String objectKey, String originalFileName,
                      String mimeType, Long sizeBytes, String checksumSha256, AppUser uploadedByUser) {
        this.purpose = purpose;
        this.objectKey = objectKey;
        this.originalFileName = originalFileName;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.checksumSha256 = checksumSha256;
        this.uploadedByUser = uploadedByUser;
    }
}
