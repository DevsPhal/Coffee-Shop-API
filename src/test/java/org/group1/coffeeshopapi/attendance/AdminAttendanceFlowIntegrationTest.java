package org.group1.coffeeshopapi.attendance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.group1.coffeeshopapi.barista.entity.Barista;
import org.group1.coffeeshopapi.barista.repository.BaristaRepository;
import org.group1.coffeeshopapi.common.enums.RegisterType;
import org.group1.coffeeshopapi.common.enums.UserStatus;
import org.group1.coffeeshopapi.common.security.SuperAdminUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Check-in and check-out as two separate admin actions, end to end through the real database queries. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminAttendanceFlowIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired BaristaRepository baristaRepository;

    private final RequestPostProcessor admin = user(new SuperAdminUserDetails("super@example.test", "unused"));
    private final LocalDateTime checkInAt = LocalDateTime.now().minusHours(3).truncatedTo(ChronoUnit.MINUTES);
    private UUID staffId;

    @BeforeEach
    void createStaffMember() {
        Barista barista = new Barista();
        barista.setFullName("Dara");
        barista.setEmail("dara-attendance@example.test");
        barista.setPassword("unused");
        barista.setStatus(UserStatus.ACTIVE);
        barista.setRegisterType(RegisterType.EMAIL);
        staffId = baristaRepository.saveAndFlush(barista).getId();
    }

    @Test
    void checkInThenCheckOutCompletesOneShiftWithoutReenteringTheCheckIn() throws Exception {
        String recordId = read(mvc.perform(post("/api/admin/attendance/check-in").with(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("baristaId", staffId, "checkInAt", checkInAt, "note", "Phone was flat")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.open").value(true))
                .andExpect(jsonPath("$.data.checkOutAt").isEmpty())
                .andExpect(jsonPath("$.data.workedMinutes").isEmpty())
                .andReturn().getResponse().getContentAsString()).at("/data/id").asText();

        // On shift: eligible for check-out, not for another check-in.
        mvc.perform(post("/api/admin/attendance/check-in").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(json("baristaId", staffId, "checkInAt", checkInAt.plusMinutes(5))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("already on shift")));
        mvc.perform(get("/api/admin/attendance").param("baristaId", staffId.toString()).param("open", "true").with(admin))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(recordId));
        mvc.perform(get("/api/admin/attendance").param("baristaId", staffId.toString()).param("open", "false").with(admin))
                .andExpect(jsonPath("$.data.totalElements").value(0));

        mvc.perform(post("/api/admin/attendance/check-out").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(json("baristaId", staffId, "checkOutAt", checkInAt.minusMinutes(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("after the check-in")));

        mvc.perform(post("/api/admin/attendance/check-out").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(json("baristaId", staffId, "checkOutAt", checkInAt.plusHours(2).plusMinutes(30))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(recordId))
                .andExpect(jsonPath("$.data.open").value(false))
                .andExpect(jsonPath("$.data.workedMinutes").value(150))
                .andExpect(jsonPath("$.data.checkInAt").value(containsString(checkInAt.toString())))
                .andExpect(jsonPath("$.data.note").value("Phone was flat"));

        mvc.perform(post("/api/admin/attendance/check-out").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(json("baristaId", staffId, "checkOutAt", LocalDateTime.now().minusMinutes(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("no active shift")));
        mvc.perform(get("/api/admin/attendance").param("baristaId", staffId.toString()).param("open", "true").with(admin))
                .andExpect(jsonPath("$.data.totalElements").value(0));
        mvc.perform(get("/api/admin/attendance").param("baristaId", staffId.toString()).param("open", "false").with(admin))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        JsonNode history = read(mvc.perform(get("/api/admin/attendance/" + recordId + "/history").with(admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).at("/data");
        assertThat(history).extracting(entry -> entry.at("/action").asText()).containsExactly("CHECK_IN", "CHECK_OUT");
        assertThat(history.get(0).at("/actorId").asText()).isEqualTo(SuperAdminUserDetails.ID.toString());
    }

    @Test
    void checkOutWithoutAnActiveShiftIsRejected() throws Exception {
        mvc.perform(post("/api/admin/attendance/check-out").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(json("baristaId", staffId, "checkOutAt", LocalDateTime.now().minusMinutes(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("no active shift")));
    }

    @Test
    void missedAttendanceStillBackfillsACompletedShiftButCannotOpenOne() throws Exception {
        mvc.perform(post("/api/admin/attendance").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(json("baristaId", staffId, "checkInAt", checkInAt)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("check-out time")));

        mvc.perform(post("/api/admin/attendance").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(json("baristaId", staffId, "checkInAt", checkInAt.minusDays(1),
                                "checkOutAt", checkInAt.minusDays(1).plusHours(8))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.open").value(false))
                .andExpect(jsonPath("$.data.workedMinutes").value(480));
    }

    private JsonNode read(String body) throws Exception {
        return objectMapper.readTree(body);
    }

    private String json(Object... keyValues) throws Exception {
        var body = new java.util.LinkedHashMap<String, String>();
        for (int i = 0; i < keyValues.length; i += 2) {
            body.put((String) keyValues[i], keyValues[i + 1].toString());
        }
        return objectMapper.writeValueAsString(body);
    }
}
