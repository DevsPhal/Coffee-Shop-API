package org.group1.coffeeshopapi.auth.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.auth.service.TokenService;
import org.group1.coffeeshopapi.common.constant.RedisKeys;
import org.group1.coffeeshopapi.common.exception.InvalidCredentialsException;
import org.group1.coffeeshopapi.common.util.JwtUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private static final Duration LOGIN_TICKET_TTL = Duration.ofMinutes(5);
    private static final String TELEGRAM_CHANNEL_SUFFIX = "|TELEGRAM";

    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;

    @Override
    public String createLoginTicket(UUID userId, boolean viaTelegram) {
        String ticket = UUID.randomUUID().toString();
        String value = viaTelegram ? userId + TELEGRAM_CHANNEL_SUFFIX : userId.toString();
        redisTemplate.opsForValue().set(RedisKeys.LOGIN_TICKET_PREFIX + ticket, value, LOGIN_TICKET_TTL);
        return ticket;
    }

    @Override
    public UUID peekLoginTicket(String ticket) {
        return UUID.fromString(readLoginTicket(ticket).replace(TELEGRAM_CHANNEL_SUFFIX, ""));
    }

    @Override
    public boolean isTelegramLoginTicket(String ticket) {
        return readLoginTicket(ticket).endsWith(TELEGRAM_CHANNEL_SUFFIX);
    }

    private String readLoginTicket(String ticket) {
        String value = redisTemplate.opsForValue().get(RedisKeys.LOGIN_TICKET_PREFIX + ticket);
        if (value == null) {
            throw new InvalidCredentialsException("Login session has expired, please log in again");
        }
        return value;
    }

    @Override
    public UUID consumeLoginTicket(String ticket) {
        UUID userId = peekLoginTicket(ticket);
        redisTemplate.delete(RedisKeys.LOGIN_TICKET_PREFIX + ticket);
        return userId;
    }

    @Override
    public void storeRefreshToken(UUID userId, String refreshToken) {
        redisTemplate.opsForValue().set(
                RedisKeys.REFRESH_TOKEN_PREFIX + userId,
                refreshToken,
                Duration.ofMillis(jwtUtil.getRefreshExpirationMs()));
    }

    @Override
    public boolean isRefreshTokenValid(UUID userId, String refreshToken) {
        String stored = redisTemplate.opsForValue().get(RedisKeys.REFRESH_TOKEN_PREFIX + userId);
        return stored != null && stored.equals(refreshToken);
    }

    @Override
    public void revokeRefreshToken(UUID userId) {
        redisTemplate.delete(RedisKeys.REFRESH_TOKEN_PREFIX + userId);
    }

    @Override
    public void denylistAccessToken(String jti, long remainingMillis) {
        if (remainingMillis <= 0) {
            return;
        }
        redisTemplate.opsForValue().set(
                RedisKeys.JWT_DENYLIST_PREFIX + jti, "1", Duration.ofMillis(remainingMillis));
    }
}
