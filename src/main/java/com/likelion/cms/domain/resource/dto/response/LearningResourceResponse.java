package com.likelion.cms.domain.resource.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import com.likelion.cms.common.type.PartType;
import com.likelion.cms.support.file.dto.response.FileAssetResponse;
import java.time.OffsetDateTime;


@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)

public class LearningResourceResponse {
    private final Long resourceId;
    private final String title;
    private final int week;
    private final PartType targetPart;
    private final Long createdBy;
    private final FileAssetResponse file;
    private final int version;
    private final OffsetDateTime createdAt;

    public static LearningResourceResponse of (Long resourceId, String title, int week,
                                               PartType targetPart, Long createdBy, FileAssetResponse file,
                                               int version, OffsetDateTime createdAt){
        return new LearningResourceResponse(resourceId,title,week,targetPart,createdBy,file,version,createdAt);
    }


}
