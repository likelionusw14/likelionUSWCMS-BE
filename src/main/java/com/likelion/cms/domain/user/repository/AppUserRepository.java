package com.likelion.cms.domain.user.repository;

import com.likelion.cms.common.type.PartType;
import com.likelion.cms.domain.user.entity.AccountStatus;
import com.likelion.cms.domain.user.entity.AppUser;
import com.likelion.cms.domain.user.entity.SystemRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

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

    Optional<AppUser> findByKakaoSubject(String kakaoSubject);

    boolean existsByStudentId(String studentId);
}