package org.group1.coffeeshopapi.auth.service;

import java.util.UUID;

public interface TokenService {
    String createLoginTicket(UUID userId);
    UUID peekLoginTicket(String ticket);
    UUID consumeLoginTicket(String ticket);
    void storeRefreshToken(UUID userId, String refreshToken);
    boolean isRefreshTokenValid(UUID userId, String refreshToken);
    void revokeRefreshToken(UUID userId);
    void denylistAccessToken(String jti, long remainingMillis);
}