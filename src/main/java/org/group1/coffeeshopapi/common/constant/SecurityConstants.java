package org.group1.coffeeshopapi.common.constant;

public class SecurityConstants {
    private SecurityConstants() {}

    public static final String JWT_HEADER = "Authorization";
    public static final String JWT_PREFIX = "Bearer ";
    public static final long JWT_EXPIRATION_MS = 86_400_000L;      // 1 day
    public static final long REFRESH_EXPIRATION_MS = 604_800_000L; // 7 days

    public static final String TELEGRAM_WEBHOOK_PATH = "/api/telegram/webhook";

    public static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/**",
            TELEGRAM_WEBHOOK_PATH,
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/login",
            "/verify",
            "/css/**",
            "/js/**"
    };
}
