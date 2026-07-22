package com.likelion.cms.domain.user.repository;

import com.likelion.cms.common.type.PartType;
import com.likelion.cms.domain.user.entity.AccountStatus;
import com.likelion.cms.domain.user.entity.AppUser;
import com.likelion.cms.domain.user.entity.SystemRole;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

// 목록 조회(GET /api/admin/accounts)의 status/role/cohortId/part/keyword 필터를
// 각각 "값이 있을 때만" 조건에 추가하는 동적 쿼리 조립기.
public final class AppUserSpecs {

    private AppUserSpecs() {
    }

    public static Specification<AppUser> withFilters(
            AccountStatus status, SystemRole role, Long cohortId, PartType part, String keyword) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("accountStatus"), status));
            }
            if (role != null) {
                predicates.add(cb.equal(root.get("systemRole"), role));
            }
            if (cohortId != null) {
                predicates.add(cb.equal(root.get("cohort").get("cohortId"), cohortId));
            }
            if (part != null) {
                predicates.add(cb.equal(root.get("part"), part));
            }
            // 이름 또는 학번에 keyword가 포함되면 매치 (대소문자 무시).
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("studentId")), pattern)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
