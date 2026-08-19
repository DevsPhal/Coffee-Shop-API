package org.group1.coffeeshopapi.common.properties;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.super-admin")
public class SuperAdminProperties {
    private String email;
    private String password;

    @PostConstruct
    void logStatus() {
        if (isConfigured()) {
            log.info("Super admin login enabled for {}", email);
        } else {
            log.warn("Super admin login is disabled: set SUPER_ADMIN_EMAIL and SUPER_ADMIN_PASSWORD to enable it");
        }
    }

    public boolean isConfigured() {
        return email != null && !email.isBlank() && password != null && !password.isBlank();
    }

    public boolean matches(String candidateEmail) {
        return isConfigured() && candidateEmail != null && email.equalsIgnoreCase(candidateEmail);
    }
}
