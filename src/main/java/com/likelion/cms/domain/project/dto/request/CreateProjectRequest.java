package com.likelion.cms.domain.project.dto.request;

import com.likelion.cms.common.type.ProjectType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.net.URI;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

// 프로젝트 등록 요청. 두 날짜가 항상 같이 오니까 여기서 바로 순서 검증 가능
// (부분 수정인 UpdateProjectRequest는 그럴 수 없어서 서비스 레이어에서 따로 검증).
//
// projectType은 String이 아니라 ProjectType enum으로 받는다 - 원래 String이었을 때
// "BACKEND" 같은 임의의 값이 그대로 저장됐다가, 조회 API(ProjectService.toResponse)가
// ProjectType.valueOf()로 파싱하는 과정에서 500이 나는 걸 실서버 검증 중 발견함.
// enum으로 받으면 유효하지 않은 값은 역직렬화 시점에 400으로 바로 걸러진다.
//
// startedMonth/endedMonth는 스펙(yyyy-MM)대로 YearMonth로 받는다. 원래 LocalDate라
// "2026-01"을 보내면 역직렬화 단계에서 400이 났고, 응답은 YearMonth라 조회한 값을
// 그대로 다시 넣을 수도 없었다. 엔티티는 date 컬럼이라 저장 시점에 그 달 1일로 변환한다.
public record CreateProjectRequest(
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 20000) String description,
        @NotNull ProjectType projectType,
        @Positive Long thumbnailAssetId,
        @Size(max = 2048) String deployUrl,
        @Size(max = 2048) String githubUrl,
        @NotNull @Positive Long cohortId,
        @NotNull YearMonth startedMonth,
        @NotNull YearMonth endedMonth,
        @NotNull @Valid List<ProjectParticipantRequest> participants
) {
    @AssertTrue(message = "deployUrl은 http 또는 https URL이어야 합니다.")
    public boolean isDeployUrlValid() {
        return isHttpUrl(deployUrl);
    }

    @AssertTrue(message = "githubUrl은 http 또는 https URL이어야 합니다.")
    public boolean isGithubUrlValid() {
        return isHttpUrl(githubUrl);
    }

    @AssertTrue(message = "endedMonth는 startedMonth보다 빠를 수 없습니다.")
    public boolean isDateRangeValid() {
        return startedMonth == null || endedMonth == null || !endedMonth.isBefore(startedMonth);
    }

    @AssertTrue(message = "participants의 userId는 중복될 수 없습니다.")
    public boolean isParticipantsUnique() {
        return hasUniqueUserIds(participants);
    }

    // UpdateProjectRequest에서도 재사용하려고 package-private static으로 뺌.
    static boolean isHttpUrl(String value) {
        if (value == null) {
            return true;
        }
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            return uri.getHost() != null
                    && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    // userId가 null인 항목은 @NotNull이 따로 잡으므로 여기선 제외하고 중복만 본다.
    // (그러지 않으면 하나의 잘못된 입력에 에러가 두 번 붙는다)
    static boolean hasUniqueUserIds(List<ProjectParticipantRequest> participants) {
        if (participants == null) {
            return true;
        }
        List<Long> userIds = participants.stream()
                .filter(Objects::nonNull)
                .map(ProjectParticipantRequest::userId)
                .filter(Objects::nonNull)
                .toList();
        return userIds.size() == userIds.stream().distinct().count();
    }
}
