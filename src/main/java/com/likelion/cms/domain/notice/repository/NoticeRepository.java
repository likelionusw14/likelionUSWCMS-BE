package com.likelion.cms.domain.notice.repository;

import com.likelion.cms.domain.notice.entity.Notice;
import com.likelion.cms.domain.notice.entity.NoticeTag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    @Query("""
            SELECT n FROM Notice n
            WHERE n.archivedAt IS NULL
            AND (:tag IS NULL OR n.tag = :tag)
            ORDER BY n.isFixed DESC, n.publishedAt DESC, n.noticeId DESC
            """)
    Page<Notice> findAllActiveByTag(@Param("tag") NoticeTag tag, Pageable pageable);
}