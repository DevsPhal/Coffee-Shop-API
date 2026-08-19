package org.group1.coffeeshopapi.mail;

public interface MailService {
    void sendOtpEmail(String to, String fullName, String otp, int expiryMinutes, String purposeLabel);
}