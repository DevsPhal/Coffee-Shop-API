package org.group1.coffeeshopapi.attendance.controller;

import org.group1.coffeeshopapi.attendance.service.AttendanceService;
import org.group1.coffeeshopapi.barista.entity.Barista;
import org.group1.coffeeshopapi.common.enums.UserStatus;
import org.group1.coffeeshopapi.common.filter.JwtAuthFilter;
import org.group1.coffeeshopapi.common.properties.CorsProperties;
import org.group1.coffeeshopapi.common.security.CustomUserDetails;
import org.group1.coffeeshopapi.common.security.CustomUserDetailsService;
import org.group1.coffeeshopapi.common.security.RestAccessDeniedHandler;
import org.group1.coffeeshopapi.common.security.RestAuthenticationEntryPoint;
import org.group1.coffeeshopapi.common.util.JwtUtil;
import org.group1.coffeeshopapi.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
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

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = BaristaAttendanceController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, RestAccessDeniedHandler.class, RestAuthenticationEntryPoint.class, CorsProperties.class})
class BaristaAttendanceControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean AttendanceService attendanceService;
    @MockitoBean JwtUtil jwtUtil;
    @MockitoBean CustomUserDetailsService userDetailsService;
    @MockitoBean StringRedisTemplate redisTemplate;
    private final UUID ownId = UUID.randomUUID();
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

    @ParameterizedTest
    @ValueSource(strings = {"check-in", "check-out"})
    void noteIsPassedForTheSignedInBarista(String action) throws Exception {
        mvc.perform(post("/api/barista/attendance/" + action).with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"note":"Leaving early — feeling unwell","baristaId":"%s"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isOk());

        if (action.equals("check-in")) verify(attendanceService).checkIn(ownId, "Leaving early — feeling unwell");
        else verify(attendanceService).checkOut(ownId, "Leaving early — feeling unwell");
    }

    @ParameterizedTest
    @ValueSource(strings = {"check-in", "check-out"})
    void anEmptyJsonBodyMeansNoNote(String action) throws Exception {
        mvc.perform(post("/api/barista/attendance/" + action).with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());

        if (action.equals("check-in")) verify(attendanceService).checkIn(ownId, null);
        else verify(attendanceService).checkOut(ownId, null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"check-in", "check-out"})
    void aNoteLongerThanTheColumnIsRejectedBeforeAnythingIsSaved(String action) throws Exception {
        mvc.perform(post("/api/barista/attendance/" + action).with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"" + "x".repeat(256) + "\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(attendanceService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"check-in", "check-out"})
    void adminsCannotUseTheBaristaSelfServiceEndpoints(String action) throws Exception {
        mvc.perform(post("/api/barista/attendance/" + action).with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"x\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(attendanceService);
    }
}
