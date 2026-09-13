package org.group1.coffeeshopapi.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.group1.coffeeshopapi.admin.entity.Admin;
import org.group1.coffeeshopapi.admin.repository.AdminRepository;
import org.group1.coffeeshopapi.barista.entity.Barista;
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
                .andExpect(jsonPath("$.data.createdBy").value(SuperAdminUserDetails.ID.toString()))
                .andReturn();

        UUID id = UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/id").asText());
        userRepository.flush();
        authUserRepository.flush();
        var stored = userRepository.findById(id).orElseThrow();
        assertThat(stored.getRole()).isEqualTo(targetRole);
        assertThat(passwordEncoder.matches(PASSWORD, stored.getPassword())).isTrue();
        assertThat(authUserRepository.findById(id).orElseThrow().getRole()).isEqualTo(targetRole);
    }

    @Test
    void adminCanCreateBaristaButCannotCreateAdmin() throws Exception {
        Admin manager = new Admin();
        manager.setFullName("Existing admin");
        manager.setEmail("existing-admin@example.test");
        manager.setPassword(passwordEncoder.encode(PASSWORD));
        manager.setStatus(UserStatus.ACTIVE);
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
