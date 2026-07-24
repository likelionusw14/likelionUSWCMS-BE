package com.likelion.cms.domain.user.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.likelion.cms.domain.user.entity.AccountStatus;
import com.likelion.cms.domain.user.entity.AppUser;
import com.likelion.cms.domain.user.entity.SystemRole;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    List<AppUser> findAllByCohort_CohortIdAndSystemRoleAndAccountStatus(
            Long cohortId,
            SystemRole systemRole,
            AccountStatus accountStatus
    );
}