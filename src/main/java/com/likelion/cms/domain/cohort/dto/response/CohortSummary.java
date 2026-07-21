package com.likelion.cms.domain.cohort.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.likelion.cms.domain.cohort.entity.Cohort;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

// Cohort 엔티티 전체 대신, 다른 도메인 응답(AccountResponse, ProjectResponse)에
// 끼워넣을 때 필요한 최소 정보만 담는 축약형 DTO.
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CohortSummary {

    private final Long cohortId;
    private final int number;
    private final String name;

    public static CohortSummary of(Long cohortId, int number, String name) {
        return new CohortSummary(cohortId, number, name);
    }

    public static CohortSummary from(Cohort cohort) {
        return new CohortSummary(cohort.getCohortId(), cohort.getNumber(), cohort.getName());
    }
}
