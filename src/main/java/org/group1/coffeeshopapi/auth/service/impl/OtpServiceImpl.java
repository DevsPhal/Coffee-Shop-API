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
import org.group1.coffeeshopapi.telegram.service.TelegramApiClient;
import org.group1.coffeeshopapi.telegram.util.TelegramFormat;
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
    private final TelegramApiClient telegramApiClient;

    @Override
    public void generateAndSend(String email, String fullName, OtpPurpose purpose, String telegramDeepLink) {
        if (isOnCooldown(email, purpose)) {
            // A still-valid code already exists — reuse it silently instead of failing.
            return;
        }
        String otp = generateAndStore(email, purpose);
        mailService.sendOtpEmail(email, fullName, otp, otpProperties.getOtpExpiryMinutes(), purpose.label(),
                telegramDeepLink);
    }

    @Override
    public void resend(String email, String fullName, OtpPurpose purpose, String telegramDeepLink) {
        if (isOnCooldown(email, purpose)) {
            throw new TooManyRequestsException("Please wait before requesting another code");
        }
        String otp = generateAndStore(email, purpose);
        mailService.sendOtpEmail(email, fullName, otp, otpProperties.getOtpExpiryMinutes(), purpose.label(),
                telegramDeepLink);
    }

    @Override
    public void resendViaTelegram(String email, String fullName, OtpPurpose purpose, Long chatId) {
        if (isOnCooldown(email, purpose)) {
            throw new TooManyRequestsException("Please wait before requesting another code");
        }
        String otp = generateAndStore(email, purpose);
        sendTelegramOtp(chatId, fullName, otp, purpose);
    }

    private boolean isOnCooldown(String email, OtpPurpose purpose) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(RedisKeys.otpCooldownKey(purpose.name(), email)));
    }

    // Generates a fresh code and does the Redis bookkeeping shared by every delivery channel.
    private String generateAndStore(String email, OtpPurpose purpose) {
        String otp = String.valueOf(100000 + RANDOM.nextInt(900000));
        String otpKey = RedisKeys.otpKey(purpose.name(), email);
        String attemptsKey = RedisKeys.otpAttemptsKey(purpose.name(), email);
        String cooldownKey = RedisKeys.otpCooldownKey(purpose.name(), email);
        Duration ttl = Duration.ofMinutes(otpProperties.getOtpExpiryMinutes());

        redisTemplate.opsForValue().set(otpKey, passwordEncoder.encode(otp), ttl);
        redisTemplate.delete(attemptsKey);
        redisTemplate.opsForValue().set(cooldownKey, "1", Duration.ofSeconds(otpProperties.getOtpResendCooldownSeconds()));

        if (otpProperties.isLogOtp()) {
            log.warn("[DEV] OTP for {} ({}): {}", email, purpose, otp);
        }
        return otp;
    }

    private void sendTelegramOtp(Long chatId, String fullName, String otp, OtpPurpose purpose) {
        String html = "👋 Hi " + TelegramFormat.escape(fullName) + ",\n\n"
                + "Your <b>" + TelegramFormat.escape(purpose.label()) + "</b> code is:\n\n<b>" + otp + "</b>\n\n"
                + "It expires in " + otpProperties.getOtpExpiryMinutes() + " minutes. Never share this code with anyone.";
        telegramApiClient.sendHtmlMessage(chatId, html);
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
