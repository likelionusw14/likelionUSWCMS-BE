package com.likelion.cms.domain.project.repository;

import com.likelion.cms.domain.project.entity.ProjectParticipation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectParticipationRepository extends JpaRepository<ProjectParticipation, Long> {

    @Query("""
            SELECT pp FROM ProjectParticipation pp
            JOIN FETCH pp.user
            WHERE pp.project.projectId IN :projectIds
            """)
    List<ProjectParticipation> findByProjectIdsWithUser(@Param("projectIds") List<Long> projectIds);

    @Query("""
            SELECT pp FROM ProjectParticipation pp
            JOIN FETCH pp.user
            WHERE pp.project.projectId = :projectId
            """)
    List<ProjectParticipation> findByProjectIdWithUser(@Param("projectId") Long projectId);
}
