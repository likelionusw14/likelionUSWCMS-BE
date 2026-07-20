package com.likelion.cms.domain.resource.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.likelion.cms.common.type.PartType;
import com.likelion.cms.support.file.dto.response.FileAssetResponse;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

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

    /**
     * verison : 낙관적 락(optimistic locking)을 위한 버전 값입니다.
     * 수정 요청 시 클라이언트가 조회 시점의 이 값을 그대로 전달해야 하며,
     * 서버에 저장된 현재 버전과 다르면 충돌로 간주해 요청이 거부됩니다.
     */
    private final int version;
    private final OffsetDateTime createdAt;

    public static LearningResourceResponse of(Long resourceId, String title, int week,
                                              PartType targetPart, Long createdBy, FileAssetResponse file,
                                              int version, OffsetDateTime createdAt) {
        return new LearningResourceResponse(resourceId, title, week, targetPart, createdBy, file, version, createdAt);
    }


}
