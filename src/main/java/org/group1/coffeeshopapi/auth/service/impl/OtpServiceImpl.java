package org.group1.coffeeshopapi.auth.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.auth.service.OtpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {
    private static final Logger log = LoggerFactory.getLogger(OtpServiceImpl.class);
    private static final String KEY_PREFIX = "otp:";

    private final SecureRandom random = new SecureRandom();
    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.auth.otp-expiry-minutes:5}")
    private long otpExpiryMinutes;

    @Value("${app.auth.log-otp:false}")
    private boolean logOtp;

    private String normalizedEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String key(String email) {
        return KEY_PREFIX + normalizedEmail(email);
    }

    @Override
    public String generateOtp(String email) {
        String normalizedEmail = normalizedEmail(email);
        String code = String.valueOf(100000 + random.nextInt(900000));

        redisTemplate.opsForValue().set(
                key(normalizedEmail),
                passwordEncoder.encode(code),
                Duration.ofMinutes(otpExpiryMinutes)
        );

        if (logOtp) {
            log.warn("Generated OTP for {}: {}", normalizedEmail, code);
        }
        return code;
    }

    @Override
    public boolean isValid(String email, String inputCode) {
        String storedHash = redisTemplate.opsForValue().get(key(email));
        if (storedHash == null) {
            return false;
        }
        return passwordEncoder.matches(inputCode.trim(), storedHash);
    }

    @Override
    public void clearOtp(String email) {
        redisTemplate.delete(key(email));
    }
}
