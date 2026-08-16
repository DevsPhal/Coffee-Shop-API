package org.group1.coffeeshopapi.auth.config;

import org.group1.coffeeshopapi.admin.entity.User;
import org.group1.coffeeshopapi.admin.repository.UserRepository;
import org.group1.coffeeshopapi.common.enums.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class SuperAdminSeeder {

    @Bean
    @Order(1)
    public CommandLineRunner seedSuperAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.auth.super-admin-email:}") String superAdminEmail,
            @Value("${app.auth.super-admin-password:}") String superAdminPassword
    ) {
        return args -> {
            if (!StringUtils.hasText(superAdminEmail)) {
                return;
            }
            String email = superAdminEmail.trim().toLowerCase();

            userRepository.findByEmail(email).ifPresentOrElse(user -> {
                if (user.getRole() != Role.SUPER_ADMIN) {
                    user.setRole(Role.SUPER_ADMIN);
                    user.setEnabled(true);
                    userRepository.save(user);
                    log.info("Promoted {} to SUPER_ADMIN.", email);
                }
            }, () -> {
                if (!StringUtils.hasText(superAdminPassword)) {
                    log.warn("SUPER_ADMIN_EMAIL is set but SUPER_ADMIN_PASSWORD is missing; skipping super admin bootstrap.");
                    return;
                }
                User user = User.builder()
                        .email(email)
                        .fullName("Super Admin")
                        .password(passwordEncoder.encode(superAdminPassword))
                        .role(Role.SUPER_ADMIN)
                        .enabled(true)
                        .build();
                userRepository.save(user);
                log.info("Created SUPER_ADMIN account for {}.", email);
            });
        };
    }
}
