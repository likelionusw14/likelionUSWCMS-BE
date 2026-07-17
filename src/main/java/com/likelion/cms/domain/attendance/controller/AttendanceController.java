package com.likelion.cms.domain.attendance.controller;

import com.likelion.cms.domain.attendance.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/attendances")
public class AttendanceController {

    private final AttendanceService attendanceService;
}
