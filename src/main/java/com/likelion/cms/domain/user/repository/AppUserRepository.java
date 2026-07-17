package com.likelion.cms.domain.user.repository;

import com.likelion.cms.domain.user.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
}
