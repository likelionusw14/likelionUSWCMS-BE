package com.likelion.cms.domain.schedule.dto.response;

public record CohortSummaryResponse(
        Long cohortId,
        Integer number,
        String name
) {
}
