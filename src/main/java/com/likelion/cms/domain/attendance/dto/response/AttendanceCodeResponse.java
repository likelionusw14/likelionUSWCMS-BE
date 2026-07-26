package com.likelion.cms.domain.attendance.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttendanceCodeResponse {

    private final Long scheduleId;
    private final String code;
    private final LocalDateTime startedAt;
    private final LocalDateTime expiresAt;

    public static AttendanceCodeResponse of(Long scheduleId, String code,
                                            LocalDateTime startedAt, LocalDateTime expiresAt) {
        return new AttendanceCodeResponse(scheduleId, code, startedAt, expiresAt);
    }
}