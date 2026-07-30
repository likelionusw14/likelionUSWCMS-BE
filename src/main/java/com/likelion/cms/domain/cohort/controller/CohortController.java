package com.likelion.cms.domain.cohort.controller;

import com.likelion.cms.domain.cohort.dto.response.CohortSummary;
import com.likelion.cms.domain.cohort.service.CohortService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cohorts")
public class CohortController {

    private final CohortService cohortService;

    @Operation(summary = "기수 목록 조회 (번호 내림차순)")
    @GetMapping
    public List<CohortSummary> getCohorts() {
        return cohortService.getCohorts();
    }
}
