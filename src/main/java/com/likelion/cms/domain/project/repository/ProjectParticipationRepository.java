package com.likelion.cms.domain.project.repository;

import com.likelion.cms.domain.project.entity.ProjectParticipation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    // 참여자 목록 교체(전체 대체)용. 파생 쿼리(deleteBy...)를 쓰면 Hibernate가 flush 시점에
    // INSERT를 DELETE보다 먼저 내보내서 uk_participation_user_project(userId, projectId)에
    // 걸릴 수 있다. JPQL 벌크 삭제는 호출 즉시 SQL이 나가므로 그 순서 문제가 없다.
    // clearAutomatically는 쓰지 않는다 - 영속성 컨텍스트를 비우면 호출부가 들고 있는
    // Project가 준영속이 되어 lazy 필드(cohort 등) 접근에서 터진다.
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM ProjectParticipation pp WHERE pp.project.projectId = :projectId")
    void deleteByProjectId(@Param("projectId") Long projectId);
}
