package org.group1.coffeeshopapi.service.impl;

import org.group1.coffeeshopapi.service.OtpService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpServiceImpl implements OtpService {
    private final SecureRandom random = new SecureRandom();
    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();
    private record OtpEntry(String code, LocalDateTime expiredAt){}
    private String normalizedEmail(String email){
        return email.trim().toLowerCase();
    }

    @Override
    public String generateOtp(String email) {
        String normalizedEmail = normalizedEmail(email);
        String codes = String.valueOf(100000 + random.nextInt(900000));
        otpStore.put(normalizedEmail, new OtpEntry(codes, LocalDateTime.now().plusMinutes(5)));
        return codes;
    }

    @Override
    public boolean isValid(String email, String inputCode) {
        String normalizedEmail = normalizedEmail(email);
        OtpEntry entry = otpStore.get(normalizedEmail);
        if (entry == null) return false;
        if (LocalDateTime.now().isAfter(entry.expiredAt())){
            otpStore.remove(normalizedEmail);
            return false;
        }
        return entry.code().equals(inputCode);
    }

    @Override
    public void clearOtp(String email) {
        otpStore.remove(normalizedEmail(email));
    }

    @Scheduled(fixedRate = 60000)
    public void cleanupExpiredOtp(){
        LocalDateTime now = LocalDateTime.now();

        otpStore.entrySet()
                .removeIf(entry -> now.isAfter(entry.getValue().expiredAt()));
    }
}
