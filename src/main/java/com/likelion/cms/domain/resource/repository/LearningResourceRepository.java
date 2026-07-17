package com.likelion.cms.domain.resource.repository;

import com.likelion.cms.domain.resource.entity.LearningResource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningResourceRepository extends JpaRepository<LearningResource, Long> {
}
