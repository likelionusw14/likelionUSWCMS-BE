package com.likelion.cms.domain.schedule.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.likelion.cms.domain.cohort.entity.Cohort;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CohortSummaryResponse {

    private Long cohortId;
    private Integer number;
    private String name;

    public static CohortSummaryResponse of(Long cohortId, Integer number, String name) {
        return new CohortSummaryResponse(cohortId, number, name);
    }

    public static CohortSummaryResponse from(Cohort cohort) {
        return of(
                cohort.getCohortId(),
                cohort.getNumber(),
                cohort.getName()
        );
    }
}