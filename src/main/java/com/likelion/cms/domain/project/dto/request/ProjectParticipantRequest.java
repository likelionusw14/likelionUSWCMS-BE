package com.likelion.cms.domain.project.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

// 프로젝트 참여자 등록/수정 요청 항목. 스펙의 ProjectParticipantRequest와 동일하게
// userId/role 둘 다 필수. role은 "팀장", "프론트엔드" 처럼 자유 문자열이라
// enum이 아니라 String이고, 길이만 엔티티 컬럼(varchar(100))에 맞춰 제한한다.
public record ProjectParticipantRequest(
        @NotNull @Positive Long userId,
        @NotBlank @Size(max = 100) String role
) {
}
