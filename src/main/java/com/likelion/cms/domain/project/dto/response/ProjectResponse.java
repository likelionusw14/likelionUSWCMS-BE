package com.likelion.cms.domain.project.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import com.likelion.cms.common.type.ProjectType;
import com.likelion.cms.support.file.dto.response.FileView;
import com.likelion.cms.domain.cohort.dto.response.CohortSummary;
import java.util.List;
import java.time.OffsetDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)

public class ProjectResponse{
    private final Long projectId;
    private final String title;
    private final String description;
    private final String deployUrl;
    private final String githubUrl;
    private final String startedMonth;
    private final String endedMonth;
    private final int version;
    private final ProjectType projectType;
    private final FileView thumbnail;
    private final CohortSummary cohort;
    private final List<ProjectParticipantResponse> participants;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;


    public static ProjectResponse of (
            Long projectId, String title, String description, String deployUrl,
            String githubUrl, String startedMonth, String endedMonth, int version,FileView thumbnail,
            ProjectType projectType, CohortSummary cohort, List<ProjectParticipantResponse> participants,
            OffsetDateTime createdAt, OffsetDateTime updatedAt){
        return new ProjectResponse(projectId,title, description, deployUrl,githubUrl,startedMonth,endedMonth,
                version, projectType, thumbnail, cohort,participants, createdAt, updatedAt);
    }
}