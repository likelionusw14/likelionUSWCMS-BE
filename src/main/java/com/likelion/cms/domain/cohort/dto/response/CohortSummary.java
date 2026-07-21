package com.likelion.cms.domain.cohort.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.likelion.cms.domain.cohort.entity.Cohort;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

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
