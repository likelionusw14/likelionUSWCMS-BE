package com.likelion.cms.domain.user.repository;

import com.likelion.cms.domain.user.entity.AppUser;
import com.likelion.cms.domain.user.entity.SystemRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

// 목록 조회에서 status/role/cohortId/part/keyword를 조합해서 필터링해야 해서
// (스펙 문서 기준) 정적 메서드 이름으로는 표현이 안 됨 -> JpaSpecificationExecutor로
// UserService에서 동적으로 조건을 조립해서 씀 (AppUserSpecs 참고).
public interface AppUserRepository extends JpaRepository<AppUser, Long>, JpaSpecificationExecutor<AppUser> {

    // "마지막 ADMIN" 판단(역할 변경/삭제 시 최소 1명은 남아야 함)에 씀.
    long countBySystemRole(SystemRole systemRole);
}
