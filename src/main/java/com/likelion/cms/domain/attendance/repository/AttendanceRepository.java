package com.likelion.cms.domain.attendance.repository;

import com.likelion.cms.domain.attendance.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
}
