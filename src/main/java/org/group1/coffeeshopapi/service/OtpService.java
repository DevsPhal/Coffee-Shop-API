package org.group1.coffeeshopapi.service;


public interface OtpService {
    String generateOtp(String email);

    boolean isValid(String email, String inputCode);

    void clearOtp(String email);
}
