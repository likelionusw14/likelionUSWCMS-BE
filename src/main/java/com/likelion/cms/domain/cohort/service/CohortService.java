package com.likelion.cms.domain.cohort.service;

import com.likelion.cms.domain.cohort.repository.CohortRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CohortService {

    private final CohortRepository cohortRepository;
}
