package org.group1.coffeeshopapi.config;

import org.group1.coffeeshopapi.attendance.controller.BaristaAttendanceController;
import org.group1.coffeeshopapi.attendance.service.AttendanceService;
import org.group1.coffeeshopapi.barista.entity.Barista;
import org.group1.coffeeshopapi.common.enums.UserStatus;
import org.group1.coffeeshopapi.common.filter.JwtAuthFilter;
import org.group1.coffeeshopapi.common.security.CustomUserDetails;
import org.group1.coffeeshopapi.common.security.CustomUserDetailsService;
import org.group1.coffeeshopapi.common.security.RestAccessDeniedHandler;
import org.group1.coffeeshopapi.common.security.RestAuthenticationEntryPoint;
import org.group1.coffeeshopapi.common.util.JwtUtil;
import org.group1.coffeeshopapi.order.controller.BaristaReportController;
import org.group1.coffeeshopapi.order.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = {BaristaAttendanceController.class, BaristaReportController.class})
@Import({SecurityConfig.class, JwtAuthFilter.class, RestAccessDeniedHandler.class, RestAuthenticationEntryPoint.class})
class BaristaOwnershipTest {
    @Autowired MockMvc mvc;
    @MockitoBean JwtUtil jwtUtil;
    @MockitoBean CustomUserDetailsService userDetailsService;
    @MockitoBean StringRedisTemplate redisTemplate;
    @MockitoBean AttendanceService attendanceService;
    @MockitoBean ReportService reportService;
    private final UUID ownId = UUID.randomUUID();
    private final String otherId = UUID.randomUUID().toString();
    private CustomUserDetails principal;

    @BeforeEach
    void signInAsBarista() {
        Barista barista = new Barista();
        barista.setId(ownId);
        barista.setEmail("barista@example.test");
        barista.setPassword("unused");
        barista.setStatus(UserStatus.ACTIVE);
        principal = new CustomUserDetails(barista);
    }

    @Test
    void attendanceAlwaysUsesSignedInBaristaEvenWhenAnotherIdIsSupplied() throws Exception {
        when(attendanceService.listOwn(eq(ownId), any())).thenReturn(Page.empty());
        mvc.perform(get("/api/barista/attendance").param("baristaId", otherId).with(user(principal)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/barista/attendance/current").param("baristaId", otherId).with(user(principal)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/barista/attendance/check-in").param("baristaId", otherId).with(user(principal)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/barista/attendance/check-out").param("baristaId", otherId).with(user(principal)))
                .andExpect(status().isOk());
        verify(attendanceService).listOwn(eq(ownId), any());
        verify(attendanceService).getCurrentOpenShift(ownId);
        verify(attendanceService).checkIn(ownId);
        verify(attendanceService).checkOut(ownId);
        verifyNoMoreInteractions(attendanceService);
    }

    @Test
    void reportAlwaysUsesSignedInBaristaEvenWhenAnotherIdIsSupplied() throws Exception {
        mvc.perform(get("/api/barista/reports/daily").param("date", "2026-09-09")
                        .param("baristaId", otherId).with(user(principal)))
                .andExpect(status().isOk());
        verify(reportService).getOwnDailyReport(ownId, LocalDate.of(2026, 9, 9));
        verifyNoMoreInteractions(reportService);
    }
}
