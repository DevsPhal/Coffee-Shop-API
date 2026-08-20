package org.group1.coffeeshopapi.mail;

public interface MailService {
    /**
     * @param telegramDeepLink when non-null, the email shows a "Connect Telegram" button linking
     *                         here; pass null to omit it (e.g. purposes other than login).
     */
    void sendOtpEmail(String to, String fullName, String otp, int expiryMinutes, String purposeLabel,
                       String telegramDeepLink);
}