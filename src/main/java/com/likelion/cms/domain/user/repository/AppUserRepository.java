package com.likelion.cms.domain.user.repository;

import com.likelion.cms.domain.user.entity.AccountStatus;
import com.likelion.cms.domain.user.entity.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    // 목록 조회에서 status 필터가 들어왔을 때 쓰는 메서드.
    // Spring Data가 메서드 이름만 보고 쿼리를 자동 생성해줌.
    Page<AppUser> findByAccountStatus(AccountStatus accountStatus, Pageable pageable);
}
