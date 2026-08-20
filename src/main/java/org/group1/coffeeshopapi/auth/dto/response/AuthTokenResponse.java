package org.group1.coffeeshopapi.auth.dto.response;

public record AuthTokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {
}