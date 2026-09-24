package org.group1.coffeeshopapi.user.mapper;

import org.group1.coffeeshopapi.admin.entity.Admin;
import org.group1.coffeeshopapi.admin.repository.AdminRepository;
import org.group1.coffeeshopapi.barista.entity.Barista;
import org.group1.coffeeshopapi.barista.repository.BaristaRepository;
import org.group1.coffeeshopapi.common.enums.RegisterType;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.enums.UserStatus;
import org.group1.coffeeshopapi.user.dto.response.UserResponse;
import org.group1.coffeeshopapi.user.entity.User;
import org.group1.coffeeshopapi.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

// Reproduces /api/users/me for real: the JWT filter loads the user in its own short session,
// so by the time it's mapped the barista's creator is an uninitialized, detached proxy.
// No @Transactional here on purpose — an open session would hide the bug.
@SpringBootTest
@ActiveProfiles("test")
class UserMapperDetachedIntegrationTest {

    @Autowired private AdminRepository adminRepository;
    @Autowired private BaristaRepository baristaRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private UserMapper userMapper;

    private Admin admin;
    private Barista barista;

    @AfterEach
    void cleanUp() {
        if (barista != null) {
            baristaRepository.deleteById(barista.getId());
        }
        if (admin != null) {
            adminRepository.deleteById(admin.getId());
        }
    }

    @Test
    void mapsAnAdminCreatedBaristaLoadedOutsideAnySession() {
        admin = new Admin();
        admin.setFullName("Chan Admin");
        admin.setEmail("detached-admin@example.test");
        admin.setPassword("unused");
        admin.setStatus(UserStatus.ACTIVE);
        admin.setRegisterType(RegisterType.EMAIL);
        admin = adminRepository.saveAndFlush(admin);

        barista = new Barista();
        barista.setFullName("Sok Dara");
        barista.setEmail("detached-barista@example.test");
        barista.setPassword("unused");
        barista.setStatus(UserStatus.ACTIVE);
        barista.setRegisterType(RegisterType.EMAIL);
        barista.setCreatedByAdmin(admin);
        barista = baristaRepository.saveAndFlush(barista);

        // Loaded and returned outside a transaction, exactly like JwtAuthFilter does.
        User loaded = userRepository.findByEmail("detached-barista@example.test").orElseThrow();

        UserResponse response = userMapper.toResponse(loaded);

        assertThat(response.createdBy()).isEqualTo(admin.getId());
        assertThat(response.createdByName()).isEqualTo("Chan Admin");
        assertThat(response.createdByRole()).isEqualTo(Role.ADMIN);
    }
}
