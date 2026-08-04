package com.likelion.cms.domain.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.likelion.cms.common.type.PartType;
import com.likelion.cms.domain.attendance.controller.AdminAttendanceController;
import com.likelion.cms.domain.attendance.dto.response.AttendanceCodeResponse;
import com.likelion.cms.domain.attendance.dto.response.AttendanceResponse;
import com.likelion.cms.domain.attendance.entity.AttendanceStatus;
import com.likelion.cms.domain.attendance.entity.CheckInSource;
import com.likelion.cms.domain.attendance.service.AdminAttendanceService;
import com.likelion.cms.domain.user.entity.SystemRole;
import com.likelion.cms.global.config.SecurityConfig;
import com.likelion.cms.global.jwt.JwtTokenProvider;
import com.likelion.cms.global.response.PageMeta;
import com.likelion.cms.global.response.PageResponse;
import com.likelion.cms.global.security.AdminAccessGuard;
import com.likelion.cms.global.security.CurrentUserPrincipal;

/**
 * #86/#87 재설계 반영: scheduleId 경로/파라미터 -> attendanceDate(date) 기준으로 재작성.
 */
@WebMvcTest(controllers = AdminAttendanceController.class)
@Import({AdminAccessGuard.class, SecurityConfig.class})
class AdminAttendanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminAttendanceService adminAttendanceService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private static final LocalDate DATE = LocalDate.of(2026, 7, 25);

    // ===================== GET /admin/attendances =====================

    @Test
    void listAttendancesReturnsOkResponse() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 7, 25, 10, 0);
        AttendanceResponse item = AttendanceResponse.of(
                100L, 1L, "정소윤", PartType.BACKEND,
                DATE, AttendanceStatus.PRESENT, now, CheckInSource.SELF_CODE,
                null, 0, now, now
        );
        PageResponse<AttendanceResponse> response = PageResponse.of(
                List.of(item), PageMeta.of(0, 20, 1, 1, false));

        when(adminAttendanceService.listAttendances(eq(DATE), any(), any(), any(), any()))
                .thenReturn(response);

        mockMvc.perform(get("/api/admin/attendances")
                        .with(authentication(adminAuthentication()))
                        .param("date", "2026-07-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].attendanceId").value(100))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void listAttendancesRejectsMissingDate() throws Exception {
        mockMvc.perform(get("/api/admin/attendances")
                        .with(authentication(adminAuthentication())))
                .andExpect(status().isBadRequest());

        verify(adminAttendanceService, never()).listAttendances(any(), any(), any(), any(), any());
    }

    @Test
    void listAttendancesRejectsMemberRole() throws Exception {
        mockMvc.perform(get("/api/admin/attendances")
                        .with(authentication(memberAuthentication()))
                        .param("date", "2026-07-25"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("C005"));

        verify(adminAttendanceService, never()).listAttendances(any(), any(), any(), any(), any());
    }

    @Test
    void listAttendancesRejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/admin/attendances")
                        .param("date", "2026-07-25"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("C004"));

        verify(adminAttendanceService, never()).listAttendances(any(), any(), any(), any(), any());
    }

    // ===================== PATCH /admin/attendances/{attendanceId} =====================

    @Test
    void updateAttendanceReturnsOkResponse() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 7, 25, 14, 0);
        AttendanceResponse response = AttendanceResponse.of(
                100L, 1L, "정소윤", PartType.BACKEND,
                DATE, AttendanceStatus.ABSENT, now, CheckInSource.ADMIN,
                "사유 기록", 1, now, now
        );

        when(adminAttendanceService.updateAttendance(eq(100L), any(), eq(7L))).thenReturn(response);

        mockMvc.perform(patch("/api/admin/attendances/100")
                        .with(authentication(adminAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "ABSENT",
                                  "memo": "사유 기록",
                                  "version": 0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ABSENT"))
                .andExpect(jsonPath("$.memo").value("사유 기록"));
    }

    @Test
    void updateAttendanceRejectsMissingStatus() throws Exception {
        mockMvc.perform(patch("/api/admin/attendances/100")
                        .with(authentication(adminAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));

        verify(adminAttendanceService, never()).updateAttendance(any(), any(), any());
    }

    @Test
    void updateAttendanceRejectsMemberRole() throws Exception {
        mockMvc.perform(patch("/api/admin/attendances/100")
                        .with(authentication(memberAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "PRESENT",
                                  "version": 0
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("C005"));

        verify(adminAttendanceService, never()).updateAttendance(any(), any(), any());
    }

    @Test
    void updateAttendanceRejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(patch("/api/admin/attendances/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "PRESENT",
                                  "version": 0
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("C004"));

        verify(adminAttendanceService, never()).updateAttendance(any(), any(), any());
    }

    // ===================== POST /admin/attendance-code/{date} =====================

    @Test
    void createOrReissueCodeReturnsCreatedResponseAndLocation() throws Exception {
        LocalDateTime startedAt = LocalDateTime.of(2026, 7, 25, 10, 0);
        AttendanceCodeResponse response = AttendanceCodeResponse.of(
                DATE, "123456", startedAt, startedAt.plusSeconds(300));

        when(adminAttendanceService.createOrReissueCode(eq(DATE), eq(7L))).thenReturn(response);

        mockMvc.perform(post("/api/admin/attendance-code/2026-07-25")
                        .with(authentication(adminAuthentication())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/admin/attendance-code/2026-07-25"))
                .andExpect(jsonPath("$.code").value("123456"));
    }

    @Test
    void createOrReissueCodeRejectsMemberRole() throws Exception {
        mockMvc.perform(post("/api/admin/attendance-code/2026-07-25")
                        .with(authentication(memberAuthentication())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("C005"));

        verify(adminAttendanceService, never()).createOrReissueCode(any(), anyLong());
    }

    // ===================== GET /admin/attendance-code/{date} =====================

    @Test
    void getCurrentCodeReturnsOkResponse() throws Exception {
        LocalDateTime startedAt = LocalDateTime.of(2026, 7, 25, 10, 0);
        AttendanceCodeResponse response = AttendanceCodeResponse.of(
                DATE, "654321", startedAt, startedAt.plusSeconds(300));

        when(adminAttendanceService.getCurrentCode(DATE)).thenReturn(response);

        mockMvc.perform(get("/api/admin/attendance-code/2026-07-25")
                        .with(authentication(adminAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("654321"));
    }

    @Test
    void getCurrentCodeRejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/admin/attendance-code/2026-07-25"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("C004"));

        verify(adminAttendanceService, never()).getCurrentCode(any());
    }

    // ===================== 인증 헬퍼 =====================

    private UsernamePasswordAuthenticationToken adminAuthentication() {
        CurrentUserPrincipal principal = new CurrentUserPrincipal(7L, SystemRole.ADMIN);
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    private UsernamePasswordAuthenticationToken memberAuthentication() {
        CurrentUserPrincipal principal = new CurrentUserPrincipal(8L, SystemRole.MEMBER);
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }
}