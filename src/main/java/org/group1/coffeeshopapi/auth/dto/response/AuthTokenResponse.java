package org.group1.coffeeshopapi.auth.dto.response;

public record AuthTokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,

        // Milliseconds, kept for clients that already parse this numerically.
        long expiresIn,

        // Same value as expiresIn, formatted for display (e.g. "1 day").
        String expiresInReadable
) {
}