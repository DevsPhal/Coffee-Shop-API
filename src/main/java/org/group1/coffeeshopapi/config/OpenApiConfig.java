package org.group1.coffeeshopapi.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Coffee Shop API",
                version = "v1",
                description = "REST API for authentication (password + OTP two-factor login), "
                        + "Redis-backed OTP verification, and Telegram account linking."
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local Development"),
                @Server(url = "https://api.590stcafe.shop", description = "Production")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Paste the access token returned by /api/auth/verify-login-otp (no 'Bearer ' prefix needed)"
)
public class OpenApiConfig {
}