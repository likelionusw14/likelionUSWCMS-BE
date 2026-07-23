package com.likelion.cms.domain.user.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.likelion.cms.common.type.PartType;
import com.likelion.cms.domain.user.dto.response.CohortSummaryDto;
import com.likelion.cms.domain.user.entity.AppUser;
import com.likelion.cms.domain.user.entity.SystemRole;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LionResponseDto {

    private Long userId;
    private String name;
    private CohortSummaryDto cohort;
    private PartType part;
    private String activityType;

    public static LionResponseDto from(AppUser appUser) {
        String activityType = appUser.getSystemRole() == SystemRole.ADMIN
                ? "OPERATOR"
                : "BABY_LION";

        return LionResponseDto.builder()
                .userId(appUser.getUserId())
                .name(appUser.getName())
                .cohort(CohortSummaryDto.from(appUser.getCohort()))
                .part(appUser.getPart())
                .activityType(activityType)
                .build();
    }
}