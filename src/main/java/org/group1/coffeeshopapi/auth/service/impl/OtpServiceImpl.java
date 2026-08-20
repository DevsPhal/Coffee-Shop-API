package org.group1.coffeeshopapi.auth.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group1.coffeeshopapi.auth.service.OtpService;
import org.group1.coffeeshopapi.common.constant.RedisKeys;
import org.group1.coffeeshopapi.common.enums.OtpPurpose;
import org.group1.coffeeshopapi.common.exception.InvalidOtpException;
import org.group1.coffeeshopapi.common.exception.TooManyRequestsException;
import org.group1.coffeeshopapi.common.properties.OtpProperties;
import org.group1.coffeeshopapi.mail.MailService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final OtpProperties otpProperties;
    private final MailService mailService;

    @Override
    public void generateAndSend(String email, String fullName, OtpPurpose purpose, String telegramDeepLink) {
        String cooldownKey = RedisKeys.otpCooldownKey(purpose.name(), email);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            // A still-valid code was already issued moments ago (e.g. a prior login attempt) —
            // reuse it silently rather than failing a legitimate flow.
            return;
        }
        send(email, fullName, purpose, telegramDeepLink);
    }

    @Override
    public void resend(String email, String fullName, OtpPurpose purpose, String telegramDeepLink) {
        String cooldownKey = RedisKeys.otpCooldownKey(purpose.name(), email);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            throw new TooManyRequestsException("Please wait before requesting another code");
        }
        send(email, fullName, purpose, telegramDeepLink);
    }

    private void send(String email, String fullName, OtpPurpose purpose, String telegramDeepLink) {
        String cooldownKey = RedisKeys.otpCooldownKey(purpose.name(), email);
        String otp = String.valueOf(100000 + RANDOM.nextInt(900000));
        String otpKey = RedisKeys.otpKey(purpose.name(), email);
        String attemptsKey = RedisKeys.otpAttemptsKey(purpose.name(), email);
        Duration ttl = Duration.ofMinutes(otpProperties.getOtpExpiryMinutes());

        redisTemplate.opsForValue().set(otpKey, passwordEncoder.encode(otp), ttl);
        redisTemplate.delete(attemptsKey);
        redisTemplate.opsForValue().set(cooldownKey, "1", Duration.ofSeconds(otpProperties.getOtpResendCooldownSeconds()));

        if (otpProperties.isLogOtp()) {
            log.warn("[DEV] OTP for {} ({}): {}", email, purpose, otp);
        }

        mailService.sendOtpEmail(email, fullName, otp, otpProperties.getOtpExpiryMinutes(), purpose.label(),
                telegramDeepLink);
    }

    @Override
    public void verify(String email, OtpPurpose purpose, String code) {
        String otpKey = RedisKeys.otpKey(purpose.name(), email);
        String attemptsKey = RedisKeys.otpAttemptsKey(purpose.name(), email);

        String attemptsValue = redisTemplate.opsForValue().get(attemptsKey);
        int attempts = attemptsValue == null ? 0 : Integer.parseInt(attemptsValue);
        if (attempts >= otpProperties.getOtpMaxAttempts()) {
            redisTemplate.delete(otpKey);
            redisTemplate.delete(attemptsKey);
            throw new TooManyRequestsException("Too many incorrect attempts, please request a new code");
        }

        String storedHash = redisTemplate.opsForValue().get(otpKey);
        if (storedHash == null) {
            throw new InvalidOtpException("Code has expired or was not found, please request a new one");
        }

        if (!passwordEncoder.matches(code, storedHash)) {
            Long updatedAttempts = redisTemplate.opsForValue().increment(attemptsKey);
            if (updatedAttempts != null && updatedAttempts == 1L) {
                Long remainingTtl = redisTemplate.getExpire(otpKey);
                if (remainingTtl != null && remainingTtl > 0) {
                    redisTemplate.expire(attemptsKey, Duration.ofSeconds(remainingTtl));
                }
            }
            throw new InvalidOtpException("Invalid verification code");
        }

        redisTemplate.delete(otpKey);
        redisTemplate.delete(attemptsKey);
    }
}
