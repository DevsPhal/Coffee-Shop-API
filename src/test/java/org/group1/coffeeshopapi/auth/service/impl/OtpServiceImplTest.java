package org.group1.coffeeshopapi.auth.service.impl;

import org.group1.coffeeshopapi.common.constant.RedisKeys;
import org.group1.coffeeshopapi.common.enums.OtpPurpose;
import org.group1.coffeeshopapi.common.exception.InvalidOtpException;
import org.group1.coffeeshopapi.common.exception.TooManyRequestsException;
import org.group1.coffeeshopapi.common.properties.OtpProperties;
import org.group1.coffeeshopapi.mail.MailService;
import org.group1.coffeeshopapi.telegram.service.TelegramApiClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the OTP verification lockout/cooldown logic — security-critical and, until now,
 * untested. See OtpServiceImpl#verify/generateAndSend/resend.
 */
@ExtendWith(MockitoExtension.class)
class OtpServiceImplTest {

    private static final String EMAIL = "user@example.com";
    private static final OtpPurpose PURPOSE = OtpPurpose.LOGIN;
    private static final String OTP_KEY = RedisKeys.otpKey(PURPOSE.name(), EMAIL);
    private static final String ATTEMPTS_KEY = RedisKeys.otpAttemptsKey(PURPOSE.name(), EMAIL);
    private static final String COOLDOWN_KEY = RedisKeys.otpCooldownKey(PURPOSE.name(), EMAIL);

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private OtpProperties otpProperties;
    @Mock private MailService mailService;
    @Mock private TelegramApiClient telegramApiClient;
    @InjectMocks private OtpServiceImpl service;

    @Test
    void verifySucceedsWithTheCorrectCodeAndClearsBothKeys() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(ATTEMPTS_KEY)).thenReturn(null);
        when(valueOperations.get(OTP_KEY)).thenReturn("hashed-otp");
        when(otpProperties.getOtpMaxAttempts()).thenReturn(5);
        when(passwordEncoder.matches("123456", "hashed-otp")).thenReturn(true);

        assertThatCode(() -> service.verify(EMAIL, PURPOSE, "123456")).doesNotThrowAnyException();

        verify(redisTemplate).delete(OTP_KEY);
        verify(redisTemplate).delete(ATTEMPTS_KEY);
    }

    @Test
    void verifyWithAnExpiredOrMissingCodeThrowsInvalidOtpException() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(ATTEMPTS_KEY)).thenReturn(null);
        when(valueOperations.get(OTP_KEY)).thenReturn(null);
        when(otpProperties.getOtpMaxAttempts()).thenReturn(5);

        assertThatThrownBy(() -> service.verify(EMAIL, PURPOSE, "123456"))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void verifyWithTheWrongCodeIncrementsAttemptsAndRejects() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(ATTEMPTS_KEY)).thenReturn("1");
        when(valueOperations.get(OTP_KEY)).thenReturn("hashed-otp");
        when(otpProperties.getOtpMaxAttempts()).thenReturn(5);
        when(passwordEncoder.matches("000000", "hashed-otp")).thenReturn(false);
        when(valueOperations.increment(ATTEMPTS_KEY)).thenReturn(2L);

        assertThatThrownBy(() -> service.verify(EMAIL, PURPOSE, "000000"))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessageContaining("Invalid");

        verify(redisTemplate, never()).delete(OTP_KEY);
    }

    @Test
    void verifyAtTheAttemptCeilingLocksOutAndClearsBothKeysWithoutCheckingTheCode() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(ATTEMPTS_KEY)).thenReturn("5");
        when(otpProperties.getOtpMaxAttempts()).thenReturn(5);

        assertThatThrownBy(() -> service.verify(EMAIL, PURPOSE, "123456"))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessageContaining("Too many");

        verify(redisTemplate).delete(OTP_KEY);
        verify(redisTemplate).delete(ATTEMPTS_KEY);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void generateAndSendSilentlyReusesAStillValidCodeInsteadOfSendingAnother() {
        when(redisTemplate.hasKey(COOLDOWN_KEY)).thenReturn(true);

        service.generateAndSend(EMAIL, "Sophal", PURPOSE, null);

        verify(mailService, never()).sendOtpEmail(any(), any(), any(), anyInt(), any(), any());
    }

    @Test
    void resendOnCooldownThrowsInsteadOfSilentlyReusingTheCode() {
        when(redisTemplate.hasKey(COOLDOWN_KEY)).thenReturn(true);

        assertThatThrownBy(() -> service.resend(EMAIL, "Sophal", PURPOSE, null))
                .isInstanceOf(TooManyRequestsException.class);

        verify(mailService, never()).sendOtpEmail(any(), any(), any(), anyInt(), any(), any());
    }
}
