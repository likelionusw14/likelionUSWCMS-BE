package com.likelion.cms.domain.cohort.repository;

import com.likelion.cms.domain.cohort.entity.Cohort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CohortRepository extends JpaRepository<Cohort, Long> {
}
