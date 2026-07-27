package com.likelion.cms.domain.resource.repository;

import com.likelion.cms.domain.resource.entity.LearningResource;
import org.springframework.data.jpa.repository.JpaRepository;
import com.likelion.cms.common.type.PartType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LearningResourceRepository extends JpaRepository<LearningResource, Long> {
    @Query("""
            SELECT r FROM LearningResource r
            JOIN FETCH r.fileAsset
            WHERE r.archivedAt IS NULL
            AND (:week IS NULL OR r.week = :week)
            AND (:targetPart IS NULL OR r.targetPart = :targetPart)
            ORDER BY r.createdAt DESC
           """)
    Page<LearningResource> findResources(@Param("week") Integer week,
                                         @Param("targetPart") PartType targetPart,
                                         Pageable pageable);
}
