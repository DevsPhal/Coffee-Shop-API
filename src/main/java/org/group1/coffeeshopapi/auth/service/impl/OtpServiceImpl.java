package org.group1.coffeeshopapi.auth.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group1.coffeeshopapi.auth.entity.EmailOtp;
import org.group1.coffeeshopapi.auth.repository.EmailOtpRepository;
import org.group1.coffeeshopapi.auth.service.OtpService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {
    private static final int OTP_EXPIRY_MINUTES = 5;

    private final SecureRandom random = new SecureRandom();
    private final EmailOtpRepository emailOtpRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.auth.log-otp:false}")
    private boolean logOtp;

    private String normalizedEmail(String email){
        return email.trim().toLowerCase();
    }

    @Override
    @Transactional
    public String generateOtp(String email) {
        String normalizedEmail = normalizedEmail(email);
        String codes = String.valueOf(100000 + random.nextInt(900000));

        emailOtpRepository.deleteByEmail(normalizedEmail);
        emailOtpRepository.save(EmailOtp.builder()
                .email(normalizedEmail)
                .otpHash(passwordEncoder.encode(codes))
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .build());

        if (logOtp) {
            log.warn("Generated OTP for {}: {}", normalizedEmail, codes);
        }
        return codes;
    }

    @Override
    @Transactional
    public boolean isValid(String email, String inputCode) {
        String normalizedEmail = normalizedEmail(email);
        EmailOtp entry = emailOtpRepository.findTopByEmailAndConsumedAtIsNullOrderByCreatedAtDesc(normalizedEmail)
                .orElse(null);

        if (entry == null) {
            return false;
        }

        if (LocalDateTime.now().isAfter(entry.getExpiresAt())){
            emailOtpRepository.delete(entry);
            return false;
        }

        return passwordEncoder.matches(inputCode.trim(), entry.getOtpHash());
    }

    @Override
    @Transactional
    public void clearOtp(String email) {
        emailOtpRepository.deleteByEmail(normalizedEmail(email));
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cleanupExpiredOtp(){
        emailOtpRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}
