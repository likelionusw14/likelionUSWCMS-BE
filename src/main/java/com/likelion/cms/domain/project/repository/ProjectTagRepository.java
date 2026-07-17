package com.likelion.cms.domain.project.repository;

import com.likelion.cms.domain.project.entity.ProjectTag;
import com.likelion.cms.domain.project.entity.ProjectTagId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectTagRepository extends JpaRepository<ProjectTag, ProjectTagId> {
}
