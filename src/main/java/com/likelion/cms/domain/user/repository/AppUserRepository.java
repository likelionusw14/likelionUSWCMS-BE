package com.likelion.cms.domain.user.repository;

import com.likelion.cms.domain.user.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByKakaoSubject(String kakaoSubject);

    boolean existsByStudentId(String studentId);
}
