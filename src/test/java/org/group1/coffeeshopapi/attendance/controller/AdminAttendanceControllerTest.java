package org.group1.coffeeshopapi.attendance.controller;

import org.group1.coffeeshopapi.attendance.dto.request.AdminCheckInRequest;
import org.group1.coffeeshopapi.attendance.dto.request.AdminCheckOutRequest;
import org.group1.coffeeshopapi.attendance.service.AttendanceService;
import org.group1.coffeeshopapi.common.filter.JwtAuthFilter;
import org.group1.coffeeshopapi.common.properties.CorsProperties;
import org.group1.coffeeshopapi.common.security.CurrentActor;
import org.group1.coffeeshopapi.common.security.CustomUserDetailsService;
import org.group1.coffeeshopapi.common.security.RestAccessDeniedHandler;
import org.group1.coffeeshopapi.common.security.RestAuthenticationEntryPoint;
import org.group1.coffeeshopapi.common.util.JwtUtil;
import org.group1.coffeeshopapi.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = AdminAttendanceController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, RestAccessDeniedHandler.class, RestAuthenticationEntryPoint.class, CorsProperties.class})
class AdminAttendanceControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean AttendanceService attendanceService;
    @MockitoBean CurrentActor currentActor;
    @MockitoBean JwtUtil jwtUtil;
    @MockitoBean CustomUserDetailsService userDetailsService;
    @MockitoBean StringRedisTemplate redisTemplate;

    private final UUID adminId = UUID.randomUUID();
    private final UUID staffId = UUID.randomUUID();

    @Test
    void checkInRecordsOnlyTheStaffMemberCheckInTimeAndNoteWithTheSignedInAdminAsActor() throws Exception {
        when(currentActor.id()).thenReturn(adminId);

        mvc.perform(post("/api/admin/attendance/check-in").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"baristaId":"%s","checkInAt":"2026-10-19T08:00","note":"Phone was flat"}
                                """.formatted(staffId)))
                .andExpect(status().isCreated());

        verify(attendanceService).adminCheckIn(
                new AdminCheckInRequest(staffId, LocalDateTime.of(2026, 10, 19, 8, 0), "Phone was flat"), adminId);
    }

    @Test
    void checkOutRecordsOnlyTheStaffMemberAndCheckOutTime() throws Exception {
        when(currentActor.id()).thenReturn(adminId);

        mvc.perform(post("/api/admin/attendance/check-out").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"baristaId":"%s","checkOutAt":"2026-10-19T17:00"}
                                """.formatted(staffId)))
                .andExpect(status().isOk());

        verify(attendanceService).adminCheckOut(
                new AdminCheckOutRequest(staffId, LocalDateTime.of(2026, 10, 19, 17, 0), null), adminId);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "check-in|{\"checkInAt\":\"2026-10-19T08:00\"}",
            "check-in|{\"baristaId\":\"%s\"}",
            "check-out|{\"checkOutAt\":\"2026-10-19T17:00\"}",
            "check-out|{\"baristaId\":\"%s\"}"
    })
    void incompleteRequestsAreRejectedBeforeReachingTheService(String testCase) throws Exception {
        String[] parts = testCase.split("\\|");
        mvc.perform(post("/api/admin/attendance/" + parts[0]).with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(parts[1].formatted(staffId)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(attendanceService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"check-in", "check-out"})
    void baristasCannotCheckOtherStaffInOrOut(String action) throws Exception {
        mvc.perform(post("/api/admin/attendance/" + action).with(user("barista").roles("BARISTA"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"baristaId":"%s","checkInAt":"2026-10-19T08:00","checkOutAt":"2026-10-19T17:00"}
                                """.formatted(staffId)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(attendanceService);
    }
}
