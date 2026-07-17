package com.likelion.cms.domain.notice.repository;

import com.likelion.cms.domain.notice.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
}
