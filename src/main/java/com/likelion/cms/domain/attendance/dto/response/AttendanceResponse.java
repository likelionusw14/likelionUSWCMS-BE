package com.likelion.cms.domain.attendance.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.likelion.cms.common.type.PartType;
import com.likelion.cms.domain.attendance.entity.Attendance;
import com.likelion.cms.domain.attendance.entity.AttendanceStatus;
import com.likelion.cms.domain.attendance.entity.CheckInSource;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttendanceResponse {

    private final Long attendanceId;
    private final Long userId;
    private final String userName;
    private final PartType part;
    private final Long scheduleId;
    private final String scheduleTitle;
    private final LocalDate scheduleDate;
    private final AttendanceStatus status;
    private final LocalDateTime checkedAt;
    private final CheckInSource checkInSource;
    private final String memo;
    private final int version;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static AttendanceResponse of(Long attendanceId, Long userId, String userName, PartType part,
                                        Long scheduleId, String scheduleTitle, LocalDate scheduleDate,
                                        AttendanceStatus status, LocalDateTime checkedAt,
                                        CheckInSource checkInSource, String memo, int version,
                                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new AttendanceResponse(attendanceId, userId, userName, part, scheduleId, scheduleTitle,
                scheduleDate, status, checkedAt, checkInSource, memo, version, createdAt, updatedAt);
    }

    public static AttendanceResponse from(Attendance attendance) {
        return of(
                attendance.getAttendanceId(),
                attendance.getUser().getUserId(),
                attendance.getUser().getName(),
                attendance.getUser().getPart(),
                attendance.getSchedule().getScheduleId(),
                attendance.getSchedule().getTitle(),
                attendance.getSchedule().getScheduleDate(),
                attendance.getStatus(),
                attendance.getCheckedAt(),
                attendance.getCheckInSource(),
                attendance.getMemo(),
                attendance.getVersion(),
                attendance.getCreatedAt(),
                attendance.getUpdatedAt()
        );
    }
}