package com.likelion.cms.domain.user.repository;

import com.likelion.cms.common.type.PartType;
import com.likelion.cms.domain.user.entity.AccountStatus;
import com.likelion.cms.domain.user.entity.AppUser;
import com.likelion.cms.domain.user.entity.SystemRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// 목록 조회에서 status/role/cohortId/part/keyword를 조합해서 필터링해야 해서
// (스펙 문서 기준) 정적 메서드 이름으로는 표현이 안 됨 -> JpaSpecificationExecutor로
// UserService에서 동적으로 조건을 조립해서 씀 (AppUserSpecs 참고).
public interface AppUserRepository extends JpaRepository<AppUser, Long>, JpaSpecificationExecutor<AppUser> {

    @Query("""
            SELECT u FROM AppUser u
            WHERE u.accountStatus = :accountStatus
            AND (:cohortId IS NULL OR u.cohort.cohortId = :cohortId)
            AND (:part IS NULL OR u.part = :part)
            AND (:role IS NULL OR u.systemRole = :role)
            ORDER BY u.cohort.number DESC, u.name ASC
            """)
    Page<AppUser> findLions(
            @Param("accountStatus") AccountStatus accountStatus,
            @Param("cohortId") Long cohortId,
            @Param("part") PartType part,
            @Param("role") SystemRole role,
            Pageable pageable
    );

    // "마지막 ADMIN" 판단(역할 변경/삭제 시 최소 1명은 남아야 함)에 씀.
    long countBySystemRole(SystemRole systemRole);

    List<AppUser> findAllByCohort_CohortIdAndSystemRoleAndAccountStatus(
            Long cohortId,
            SystemRole systemRole,
            AccountStatus accountStatus
    );

    // #38(카카오 로그인) 쪽에서 추가된 메서드.
    Optional<AppUser> findByKakaoSubject(String kakaoSubject);

    boolean existsByStudentId(String studentId);
}
