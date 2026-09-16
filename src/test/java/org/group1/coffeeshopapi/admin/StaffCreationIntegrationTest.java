package org.group1.coffeeshopapi.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.group1.coffeeshopapi.admin.entity.Admin;
import org.group1.coffeeshopapi.admin.repository.AdminRepository;
import org.group1.coffeeshopapi.barista.entity.Barista;
import org.group1.coffeeshopapi.common.enums.RegisterType;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.enums.UserStatus;
import org.group1.coffeeshopapi.common.security.CustomUserDetails;
import org.group1.coffeeshopapi.common.security.SuperAdminUserDetails;
import org.group1.coffeeshopapi.user.repository.AuthUserRepository;
import org.group1.coffeeshopapi.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Real controllers, security and persistence, using an isolated H2 database and rolled-back records. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StaffCreationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired AdminRepository adminRepository;
    @Autowired AuthUserRepository authUserRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private static final String PASSWORD = "StaffTest!234";
    private static final String BODY = """
            {"fullName":"Staff creation test","email":"staff-create@example.test","password":"StaffTest!234"}
            """;

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "BARISTA"})
    void superAdminCreatesBothKindsOfActiveStaff(Role targetRole) throws Exception {
        String resource = targetRole == Role.ADMIN ? "admins" : "baristas";
        var result = mvc.perform(post("/api/admin/" + resource)
                        .with(user(new SuperAdminUserDetails("super@example.test", "unused")))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value(targetRole.name()))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).at("/data");
        UUID id = UUID.fromString(data.at("/id").asText());
        userRepository.flush();
        authUserRepository.flush();
        var stored = userRepository.findById(id).orElseThrow();
        assertThat(stored.getRole()).isEqualTo(targetRole);
        assertThat(passwordEncoder.matches(PASSWORD, stored.getPassword())).isTrue();
        assertThat(authUserRepository.findById(id).orElseThrow().getRole()).isEqualTo(targetRole);

        // Admin.createdBy is a plain audit id, so it still captures the Super Admin's reserved id
        // even though the Super Admin has no row of its own (see Admin's javadoc). Barista
        // .createdByAdmin is a real FK to "admins" instead, and there's nothing there for it to
        // point to (see AdminRepository#referenceOrNull) — null is the correct, documented
        // outcome for a Super-Admin-created barista, not a bug.
        if (targetRole == Role.ADMIN) {
            assertThat(data.at("/createdBy").asText()).isEqualTo(SuperAdminUserDetails.ID.toString());
        } else {
            assertThat(data.at("/createdBy").isNull()).isTrue();
        }
    }

    @Test
    void adminCanCreateBaristaButCannotCreateAdmin() throws Exception {
        Admin manager = new Admin();
        manager.setFullName("Existing admin");
        manager.setEmail("existing-admin@example.test");
        manager.setPassword(passwordEncoder.encode(PASSWORD));
        manager.setStatus(UserStatus.ACTIVE);
        manager.setRegisterType(RegisterType.EMAIL);
        adminRepository.saveAndFlush(manager);
        var principal = new CustomUserDetails(manager);

        mvc.perform(post("/api/admin/admins").with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        assertThat(userRepository.existsByEmail("staff-create@example.test")).isFalse();

        mvc.perform(post("/api/admin/baristas").with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("BARISTA"));
        assertThat(userRepository.findByEmail("staff-create@example.test").orElseThrow()).isInstanceOf(Barista.class);
    }
}
