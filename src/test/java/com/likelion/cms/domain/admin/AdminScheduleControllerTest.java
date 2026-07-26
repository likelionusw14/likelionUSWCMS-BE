package com.likelion.cms.domain.admin;

import com.likelion.cms.domain.schedule.controller.AdminScheduleController;
import com.likelion.cms.domain.cohort.dto.response.CohortSummary;
import com.likelion.cms.domain.schedule.dto.response.ScheduleResponse;
import com.likelion.cms.domain.schedule.service.ScheduleService;
import com.likelion.cms.domain.user.entity.SystemRole;
import com.likelion.cms.global.config.SecurityConfig;
import com.likelion.cms.global.security.AdminAccessGuard;
import com.likelion.cms.global.security.CurrentUserPrincipal;
import com.likelion.cms.global.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminScheduleController.class)
@Import({AdminAccessGuard.class, SecurityConfig.class})
class AdminScheduleControllerTest {

    private static final String IDEMPOTENCY_KEY = "123e4567-e89b-12d3-a456-426614174000";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ScheduleService scheduleService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void createScheduleReturnsCreatedResponseAndLocation() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 7, 20, 10, 0);
        ScheduleResponse response = ScheduleResponse.of(
                20L, "정기 세션", "설명",
                CohortSummary.of(1L, 14, "14기"),
                LocalDate.of(2026, 8, 1), false, LocalTime.of(19, 0), "미래 101호",
                0, now, now
        );
        when(scheduleService.create(any(), eq(7L))).thenReturn(response);

        mockMvc.perform(post("/api/admin/schedules")
                        .with(authentication(adminAuthentication()))
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "정기 세션",
                                  "cohortId": 1,
                                  "scheduleDate": "2026-08-01",
                                  "isAllDay": false,
                                  "startTime": "19:00:00",
                                  "location": "미래 101호"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/schedules/20"))
                .andExpect(jsonPath("$.scheduleId").value(20));
    }

    @Test
    void createScheduleRejectsMissingIdempotencyKey() throws Exception {
        mockMvc.perform(post("/api/admin/schedules")
                        .with(authentication(adminAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "정기 세션",
                                  "cohortId": 1,
                                  "scheduleDate": "2026-08-01",
                                  "isAllDay": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
        verify(scheduleService, never()).create(any(), any());
    }

    @Test
    void createScheduleRejectsMemberRole() throws Exception {
        mockMvc.perform(post("/api/admin/schedules")
                        .with(authentication(memberAuthentication()))
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "정기 세션",
                                  "cohortId": 1,
                                  "scheduleDate": "2026-08-01",
                                  "isAllDay": true
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("C005"));
        verify(scheduleService, never()).create(any(), any());
    }

    @Test
    void createScheduleRejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(post("/api/admin/schedules")
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "정기 세션",
                                  "cohortId": 1,
                                  "scheduleDate": "2026-08-01",
                                  "isAllDay": true
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("C004"));
        verify(scheduleService, never()).create(any(), any());
    }

    @Test
    void createScheduleRejectsBlankTitle() throws Exception {
        mockMvc.perform(post("/api/admin/schedules")
                        .with(authentication(adminAuthentication()))
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "cohortId": 1,
                                  "scheduleDate": "2026-08-01",
                                  "isAllDay": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
        verify(scheduleService, never()).create(any(), any());
    }

    @Test
    void updateScheduleReturnsOkResponse() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 7, 20, 10, 0);
        ScheduleResponse response = ScheduleResponse.of(
                10L, "변경된 제목", "기존 설명",
                CohortSummary.of(1L, 14, "14기"),
                LocalDate.of(2026, 8, 1), false, LocalTime.of(19, 0), "기존 장소",
                2, now, now
        );
        when(scheduleService.update(eq(10L), any(), eq(7L))).thenReturn(response);

        mockMvc.perform(patch("/api/admin/schedules/10")
                        .with(authentication(adminAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version": 1,
                                  "title": "변경된 제목"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduleId").value(10))
                .andExpect(jsonPath("$.title").value("변경된 제목"));
    }

    @Test
    void updateScheduleRejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(patch("/api/admin/schedules/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version": 1,
                                  "title": "변경된 제목"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("C004"));
        verify(scheduleService, never()).update(any(), any(), any());
    }

    @Test
    void updateSchedulePreservesExplicitNullNotSentForStartTime() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 7, 20, 10, 0);
        ScheduleResponse response = ScheduleResponse.of(
                10L, "기존 제목", "기존 설명",
                CohortSummary.of(1L, 14, "14기"),
                LocalDate.of(2026, 8, 1), true, null, "기존 장소",
                2, now, now
        );
        when(scheduleService.update(eq(10L), any(), eq(7L))).thenReturn(response);

        mockMvc.perform(patch("/api/admin/schedules/10")
                        .with(authentication(adminAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version": 1,
                                  "isAllDay": true
                                }
                                """))
                .andExpect(status().isOk());

        var requestCaptor = ArgumentCaptor.forClass(
                com.likelion.cms.domain.schedule.dto.request.UpdateScheduleRequest.class);
        verify(scheduleService).update(eq(10L), requestCaptor.capture(), eq(7L));
        assertThat(requestCaptor.getValue().isAllDayProvided()).isTrue();
        assertThat(requestCaptor.getValue().isTitleProvided()).isFalse();
    }

    @Test
    void updateScheduleRejectsOnlyVersionField() throws Exception {
        mockMvc.perform(patch("/api/admin/schedules/10")
                        .with(authentication(adminAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version": 1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
        verify(scheduleService, never()).update(any(), any(), any());
    }

    @Test
    void deleteScheduleReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/admin/schedules/10")
                        .with(authentication(adminAuthentication())))
                .andExpect(status().isNoContent());
        verify(scheduleService).delete(10L, 7L);
    }

    @Test
    void deleteScheduleRejectsMemberRole() throws Exception {
        mockMvc.perform(delete("/api/admin/schedules/10")
                        .with(authentication(memberAuthentication())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("C005"));
        verify(scheduleService, never()).delete(any(), any());
    }

    @Test
    void createScheduleRejectsMalformedIdempotencyKey() throws Exception {
        mockMvc.perform(post("/api/admin/schedules")
                        .with(authentication(adminAuthentication()))
                        .header("Idempotency-Key", "not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "title": "정기 세션",
                              "cohortId": 1,
                              "scheduleDate": "2026-08-01",
                              "isAllDay": true
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
        verify(scheduleService, never()).create(any(), any());
    }

    private UsernamePasswordAuthenticationToken adminAuthentication() {
        CurrentUserPrincipal principal = new CurrentUserPrincipal(7L, SystemRole.ADMIN);
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    private UsernamePasswordAuthenticationToken memberAuthentication() {
        CurrentUserPrincipal principal = new CurrentUserPrincipal(8L, SystemRole.MEMBER);
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }
}