package com.likelion.cms.domain.project.repository;

import com.likelion.cms.domain.project.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {
}
