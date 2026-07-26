package com.likelion.cms.domain.attendance.entity;

public enum AdminSettableAttendanceStatus {
    PRESENT,
    LATE,
    ABSENT;

    public AttendanceStatus toEntityStatus() {
        return AttendanceStatus.valueOf(this.name());
    }
}