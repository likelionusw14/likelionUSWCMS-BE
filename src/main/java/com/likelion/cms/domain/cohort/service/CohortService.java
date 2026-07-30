package com.likelion.cms.domain.cohort.service;

import com.likelion.cms.domain.cohort.dto.response.CohortSummary;
import com.likelion.cms.domain.cohort.repository.CohortRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CohortService {

    private final CohortRepository cohortRepository;

    // 기수 번호 내림차순(최신 기수 먼저) 전체 목록. 회원가입 시 기수 선택 드롭다운 등에 쓴다.
    public List<CohortSummary> getCohorts() {
        return cohortRepository.findAllByOrderByNumberDesc().stream()
                .map(CohortSummary::from)
                .toList();
    }
}
