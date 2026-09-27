package org.group1.coffeeshopapi.auth.service;

import java.util.UUID;

public interface TokenService {
    default String createLoginTicket(UUID userId) {
        return createLoginTicket(userId, false);
    }

    // viaTelegram marks a ticket whose code went to Telegram, so a resend uses the same channel.
    String createLoginTicket(UUID userId, boolean viaTelegram);
    boolean isTelegramLoginTicket(String ticket);
    UUID peekLoginTicket(String ticket);
    UUID consumeLoginTicket(String ticket);
    void storeRefreshToken(UUID userId, String refreshToken);
    boolean isRefreshTokenValid(UUID userId, String refreshToken);
    void revokeRefreshToken(UUID userId);
    void denylistAccessToken(String jti, long remainingMillis);
}