package com.likelion.cms.domain.attendance.service;

import java.time.LocalDateTime;

public record AttendanceCodeCacheValue(
        String code,
        LocalDateTime startedAt
) {
}