package org.group1.coffeeshopapi.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.service.OtpService;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class OtpServiceImpl implements OtpService {
    private final SecureRandom random = new SecureRandom();

    @Override
    public String generateOtp() {
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
}
