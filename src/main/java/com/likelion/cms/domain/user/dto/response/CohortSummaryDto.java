package com.likelion.cms.domain.user.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.likelion.cms.domain.cohort.entity.Cohort;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CohortSummaryDto {

    private Long cohortId;
    private Integer number;
    private String name;

    public static CohortSummaryDto from(Cohort cohort) {
        return CohortSummaryDto.builder()
                .cohortId(cohort.getCohortId())
                .number(cohort.getNumber())
                .name(cohort.getName())
                .build();
    }
}