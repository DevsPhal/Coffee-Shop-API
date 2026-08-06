package org.group1.coffeeshopapi.auth.service;

public interface EmailService {

    void sendOtpEmail(String email, String userName, String otp);
}
