package org.group1.coffeeshopapi.service;

public interface EmailService {
    void sendOtpEmail(String email, String userName, String otp);
}
