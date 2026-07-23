package com.likelion.cms.domain.project.repository;

import com.likelion.cms.domain.project.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    @Query("""
            SELECT p FROM Project p
            JOIN FETCH p.cohort
            LEFT JOIN FETCH p.thumbnailAsset
            WHERE (:cohortId IS NULL OR p.cohort.cohortId = :cohortId)
            AND (:projectTypes IS NULL OR p.projectType IN :projectTypes)
            ORDER BY p.createdAt DESC
            """)
    Page<Project> findProjects(@Param("cohortId") Long cohortId,
                               @Param("projectTypes") List<String> projectTypes,
                               Pageable pageable);

}
