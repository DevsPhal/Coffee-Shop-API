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
                description = "REST API for a coffee shop: authentication (password + OTP two-factor "
                        + "login, Redis-backed OTP, Telegram account linking), catalog (categories, "
                        + "products, discounts, Excel import), FIFO/LIFO inventory (stock in/out, "
                        + "low-stock report), POS + self-service ordering with Cash and Bakong KHQR "
                        + "payment, banners, events, expenses, finance/sales reporting, and admin/staff "
                        + "account management. See the tag list below for the full surface area, "
                        + "grouped by who can call it (Super Admin / Admin / Barista / Customer / public)."
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