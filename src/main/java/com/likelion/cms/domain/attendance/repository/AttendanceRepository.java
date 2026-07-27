package com.likelion.cms.domain.attendance.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.likelion.cms.common.type.PartType;
import com.likelion.cms.domain.attendance.entity.Attendance;
import com.likelion.cms.domain.attendance.entity.AttendanceStatus;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    @EntityGraph(attributePaths = {"user", "schedule"})
    @Query("""
            SELECT a FROM Attendance a
            WHERE a.schedule.scheduleId = :scheduleId
              AND (:part IS NULL OR a.user.part = :part)
              AND (:userId IS NULL OR a.user.userId = :userId)
              AND (:status IS NULL OR a.status = :status)
            """)
    Page<Attendance> searchForAdmin(
            @Param("scheduleId") Long scheduleId,
            @Param("part") PartType part,
            @Param("userId") Long userId,
            @Param("status") AttendanceStatus status,
            Pageable pageable
    );

    @Query("SELECT a.user.userId FROM Attendance a WHERE a.schedule.scheduleId = :scheduleId")
    List<Long> findUserIdsByScheduleId(@Param("scheduleId") Long scheduleId);

    Optional<Attendance> findByUser_UserIdAndSchedule_ScheduleId(Long userId, Long scheduleId);

    @Query("""
            SELECT a FROM Attendance a
            JOIN FETCH a.schedule s
            WHERE a.user.userId = :userId
            ORDER BY s.scheduleDate DESC, a.attendanceId DESC
            """)
    Page<Attendance> findAllByUserId(@Param("userId") Long userId, Pageable pageable);
}
