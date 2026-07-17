package com.likelion.cms.domain.project.repository;

import com.likelion.cms.domain.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
}
