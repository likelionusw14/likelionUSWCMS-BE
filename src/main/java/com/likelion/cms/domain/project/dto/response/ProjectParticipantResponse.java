package com.likelion.cms.domain.project.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)

public class ProjectParticipantResponse{
    private final Long userId;
    private final String name;
    private final String role;

    public static ProjectParticipantResponse of(Long userId, String name, String role){
        return new ProjectParticipantResponse(userId,name,role);
    }

}