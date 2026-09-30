package org.group1.coffeeshopapi.auth.dto.response;

// loginDomain is null when not configured, meaning the frontend shouldn't check its host.
public record TelegramWidgetConfigResponse(String botUsername, String loginDomain) {
}
