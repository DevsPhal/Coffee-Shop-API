package org.group1.coffeeshopapi.auth.service.impl;

import org.group1.coffeeshopapi.common.constant.RedisKeys;
import org.group1.coffeeshopapi.common.exception.InvalidCredentialsException;
import org.group1.coffeeshopapi.common.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the Redis-backed login ticket / refresh token / denylist bookkeeping — security-critical
 * and, until now, untested. See TokenServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class TokenServiceImplTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private JwtUtil jwtUtil;
    @InjectMocks private TokenServiceImpl service;

    @Test
    void consumingALoginTicketReturnsItsUserIdAndDeletesItSoItCannotBeReused() {
        UUID userId = UUID.randomUUID();
        String ticket = "some-ticket";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisKeys.LOGIN_TICKET_PREFIX + ticket)).thenReturn(userId.toString());

        UUID resolved = service.consumeLoginTicket(ticket);

        assertThat(resolved).isEqualTo(userId);
        verify(redisTemplate).delete(RedisKeys.LOGIN_TICKET_PREFIX + ticket);
    }

    @Test
    void peekingAnExpiredOrUnknownLoginTicketThrowsInvalidCredentials() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisKeys.LOGIN_TICKET_PREFIX + "bad-ticket")).thenReturn(null);

        assertThatThrownBy(() -> service.peekLoginTicket("bad-ticket"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void aRefreshTokenIsValidOnlyWhenItMatchesTheOneStoredForThatUser() {
        UUID userId = UUID.randomUUID();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisKeys.REFRESH_TOKEN_PREFIX + userId)).thenReturn("the-real-token");

        assertThat(service.isRefreshTokenValid(userId, "the-real-token")).isTrue();
        assertThat(service.isRefreshTokenValid(userId, "a-forged-token")).isFalse();
    }

    @Test
    void aRefreshTokenIsInvalidWhenNoneIsStoredAtAllForThatUser() {
        UUID userId = UUID.randomUUID();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisKeys.REFRESH_TOKEN_PREFIX + userId)).thenReturn(null);

        assertThat(service.isRefreshTokenValid(userId, "anything")).isFalse();
    }

    @Test
    void denylistingAnAlreadyExpiredAccessTokenIsANoOp() {
        service.denylistAccessToken("some-jti", 0);
        service.denylistAccessToken("some-jti", -5);

        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void denylistingAStillValidAccessTokenStoresItUntilItWouldHaveExpiredAnyway() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        service.denylistAccessToken("some-jti", 30_000);

        verify(valueOperations).set(eq(RedisKeys.JWT_DENYLIST_PREFIX + "some-jti"), eq("1"), eq(Duration.ofMillis(30_000)));
    }

    @Test
    void revokingARefreshTokenDeletesItsRedisEntry() {
        UUID userId = UUID.randomUUID();

        service.revokeRefreshToken(userId);

        verify(redisTemplate).delete(RedisKeys.REFRESH_TOKEN_PREFIX + userId);
    }
}
