package org.group1.coffeeshopapi.common.constant;

public class SecurityConstants {
    private SecurityConstants() {}

    public static final String JWT_HEADER = "Authorization";
    public static final String JWT_PREFIX = "Bearer ";
    public static final long JWT_EXPIRATION_MS = 86_400_000L;
    public static final long REFRESH_EXPIRATION_MS = 604_800_000L;

    public static final String TELEGRAM_WEBHOOK_PATH = "/api/telegram/webhook";

    public static final String[] PUBLIC_READ_ENDPOINTS = {
            "/api/banners/**",
            "/api/events/**",
            "/api/products/**",
            "/api/categories/**",
            "/api/tables/**"
    };

    public static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/**",
            TELEGRAM_WEBHOOK_PATH,
            "/ws/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/login",
            "/verify",
            "/css/**",
            "/js/**",
            "/telegramWidget.html"
    };
}
