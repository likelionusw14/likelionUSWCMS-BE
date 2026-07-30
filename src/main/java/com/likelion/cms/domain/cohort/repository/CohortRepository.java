package com.likelion.cms.domain.cohort.repository;

import com.likelion.cms.domain.cohort.entity.Cohort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CohortRepository extends JpaRepository<Cohort, Long> {
    List<Cohort> findAllByOrderByNumberDesc();
}
