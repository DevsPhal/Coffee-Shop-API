package org.group1.coffeeshopapi.auth.dto.response;

public record AuthTokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,

        // Milliseconds, kept for clients that already parse this numerically.
        long expiresIn,

        // Same value as expiresIn, broken into days/hours/minutes for display, e.g. "1 day" or "23 hours 59 minutes".
        String expiresInReadable
) {
}