package com.likelion.cms.domain.user.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.likelion.cms.common.type.PartType;
import com.likelion.cms.domain.cohort.dto.response.CohortSummary;
import com.likelion.cms.domain.user.entity.AppUser;
import com.likelion.cms.domain.user.entity.SystemRole;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LionResponse {

    private final Long userId;
    private final String name;
    private final CohortSummary cohort;
    private final PartType part;
    private final String activityType;

    public static LionResponse of(Long userId, String name, CohortSummary cohort,
                                  PartType part, String activityType) {
        return new LionResponse(userId, name, cohort, part, activityType);
    }

    public static LionResponse from(AppUser appUser) {
        String activityType = appUser.getSystemRole() == SystemRole.ADMIN
                ? "OPERATOR"
                : "BABY_LION";

        CohortSummary cohortSummary = CohortSummary.of(
                appUser.getCohort().getCohortId(),
                appUser.getCohort().getNumber(),
                appUser.getCohort().getName()
        );

        return of(
                appUser.getUserId(),
                appUser.getName(),
                cohortSummary,
                appUser.getPart(),
                activityType
        );
    }
}