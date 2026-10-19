package org.group1.coffeeshopapi.attendance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.group1.coffeeshopapi.barista.entity.Barista;
import org.group1.coffeeshopapi.barista.repository.BaristaRepository;
import org.group1.coffeeshopapi.common.enums.RegisterType;
import org.group1.coffeeshopapi.common.enums.UserStatus;
import org.group1.coffeeshopapi.common.security.CustomUserDetails;
import org.group1.coffeeshopapi.common.security.SuperAdminUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A barista's own check-in/check-out notes, saved on the right shift and readable by admins. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BaristaAttendanceNotesIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired BaristaRepository baristaRepository;

    private final RequestPostProcessor admin = user(new SuperAdminUserDetails("super@example.test", "unused"));

    @Test
    void eachShiftKeepsItsOwnNoteThroughCheckInAndCheckOut() throws Exception {
        Barista barista = new Barista();
        barista.setFullName("Dara");
        barista.setEmail("dara-notes@example.test");
        barista.setPassword("unused");
        barista.setStatus(UserStatus.ACTIVE);
        barista.setRegisterType(RegisterType.EMAIL);
        barista = baristaRepository.saveAndFlush(barista);
        RequestPostProcessor dara = user(new CustomUserDetails(barista));

        // Shift 1: note at check-in, nothing new at check-out — the check-in note stays.
        String first = idOf(mvc.perform(withNote(post("/api/barista/attendance/check-in"), "Arrived late — bus was delayed").with(dara))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.note").value("Arrived late — bus was delayed"))
                .andExpect(jsonPath("$.data.open").value(true))
                .andReturn().getResponse().getContentAsString());
        mvc.perform(get("/api/barista/attendance/current").with(dara))
                .andExpect(jsonPath("$.data.id").value(first))
                .andExpect(jsonPath("$.data.note").value("Arrived late — bus was delayed"));
        mvc.perform(post("/api/barista/attendance/check-out").with(dara))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(first))
                .andExpect(jsonPath("$.data.open").value(false))
                .andExpect(jsonPath("$.data.workedMinutes").isNumber())
                .andExpect(jsonPath("$.data.note").value("Arrived late — bus was delayed"));

        // Shift 2: its own note, edited at check-out.
        String second = idOf(mvc.perform(withNote(post("/api/barista/attendance/check-in"), "Covering for Sopheak").with(dara))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.note").value("Covering for Sopheak"))
                .andReturn().getResponse().getContentAsString());
        assertThat(second).isNotEqualTo(first);
        mvc.perform(withNote(post("/api/barista/attendance/check-out"), "Covering for Sopheak. Left early — feeling unwell").with(dara))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(second))
                .andExpect(jsonPath("$.data.note").value("Covering for Sopheak. Left early — feeling unwell"));

        // Admin and barista history both show each note on its own shift.
        for (var listing : new MockHttpServletRequestBuilder[]{
                get("/api/admin/attendance").param("baristaId", barista.getId().toString()).with(admin),
                get("/api/barista/attendance").with(dara)}) {
            mvc.perform(listing)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").value(2))
                    .andExpect(jsonPath("$.data.content[?(@.id == '" + first + "')].note").value(contains("Arrived late — bus was delayed")))
                    .andExpect(jsonPath("$.data.content[?(@.id == '" + second + "')].note")
                            .value(contains("Covering for Sopheak. Left early — feeling unwell")));
        }

        JsonNode history = objectMapper.readTree(mvc.perform(get("/api/admin/attendance/" + first + "/history").with(admin))
                .andReturn().getResponse().getContentAsString()).at("/data");
        assertThat(history).extracting(entry -> entry.at("/action").asText()).containsExactly("CHECK_IN", "CHECK_OUT");
        assertThat(history.get(0).at("/note").asText()).isEqualTo("Arrived late — bus was delayed");
        assertThat(history.get(0).at("/actorId").asText()).isEqualTo(barista.getId().toString());
    }

    private MockHttpServletRequestBuilder withNote(MockHttpServletRequestBuilder request, String note) throws Exception {
        return request.contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of("note", note)));
    }

    private String idOf(String body) throws Exception {
        return objectMapper.readTree(body).at("/data/id").asText();
    }
}
