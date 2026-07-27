package com.likelion.cms.domain.cohort.controller;

import com.likelion.cms.domain.cohort.service.CohortService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cohorts")
public class CohortController {

    private final CohortService cohortService;
}
