package org.group1.coffeeshopapi.auth.service;

import org.group1.coffeeshopapi.common.enums.OtpPurpose;

public interface OtpService {
    default void generateAndSend(String email, String fullName, OtpPurpose purpose) {
        generateAndSend(email, fullName, purpose, null);
    }

    void generateAndSend(String email, String fullName, OtpPurpose purpose, String telegramDeepLink);

    default void resend(String email, String fullName, OtpPurpose purpose) {
        resend(email, fullName, purpose, null);
    }

    void resend(String email, String fullName, OtpPurpose purpose, String telegramDeepLink);

    void generateAndSendViaTelegram(String email, String fullName, OtpPurpose purpose, Long chatId);

    void resendViaTelegram(String email, String fullName, OtpPurpose purpose, Long chatId);

    void verify(String email, OtpPurpose purpose, String code);
}
