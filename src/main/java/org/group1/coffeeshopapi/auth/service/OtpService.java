package org.group1.coffeeshopapi.auth.service;

import org.group1.coffeeshopapi.common.enums.OtpPurpose;

public interface OtpService {
    // Sends a code. If one is already active (cooldown running), silently reuses it instead of
    // failing — so login/register/forgot-password never break just because a code was just sent.
    default void generateAndSend(String email, String fullName, OtpPurpose purpose) {
        generateAndSend(email, fullName, purpose, null);
    }

    // Same, but the email also offers a "Connect Telegram" button when telegramDeepLink is set.
    void generateAndSend(String email, String fullName, OtpPurpose purpose, String telegramDeepLink);

    // A user-clicked resend — unlike generateAndSend, this throws on cooldown so repeated clicks
    // get real feedback.
    default void resend(String email, String fullName, OtpPurpose purpose) {
        resend(email, fullName, purpose, null);
    }

    // Same as resend, with an optional Telegram deep link.
    void resend(String email, String fullName, OtpPurpose purpose, String telegramDeepLink);

    // Same as resend, but delivered via Telegram instead of email.
    void resendViaTelegram(String email, String fullName, OtpPurpose purpose, Long chatId);

    void verify(String email, OtpPurpose purpose, String code);
}
