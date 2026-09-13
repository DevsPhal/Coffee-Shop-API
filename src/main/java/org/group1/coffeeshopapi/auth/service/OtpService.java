package org.group1.coffeeshopapi.auth.service;

import org.group1.coffeeshopapi.common.enums.OtpPurpose;

public interface OtpService {
    /**
     * Sends a code for the given purpose. If an unexpired code already exists (e.g. a resend
     * cooldown is active), this is a silent no-op — the existing code is still valid and usable,
     * so normal flows (login, register, forgot-password) never fail just because a code was
     * already issued moments ago.
     */
    default void generateAndSend(String email, String fullName, OtpPurpose purpose) {
        generateAndSend(email, fullName, purpose, null);
    }

    /**
     * Same as {@link #generateAndSend(String, String, OtpPurpose)}, but the email also offers a
     * "Connect Telegram" button linking to {@code telegramDeepLink} when non-null.
     */
    void generateAndSend(String email, String fullName, OtpPurpose purpose, String telegramDeepLink);

    /**
     * Explicit user-initiated resend. Unlike {@link #generateAndSend}, this throws while the
     * cooldown is active so repeated clicks get real feedback instead of silently doing nothing.
     */
    default void resend(String email, String fullName, OtpPurpose purpose) {
        resend(email, fullName, purpose, null);
    }

    /** Same as {@link #resend(String, String, OtpPurpose)}, with an optional Telegram deep link. */
    void resend(String email, String fullName, OtpPurpose purpose, String telegramDeepLink);

    /**
     * Same code/cooldown/attempts bookkeeping as {@link #resend}, but delivered as a Telegram chat
     * message instead of an email — used to resend a customer's registration code once they've
     * linked Telegram mid-registration (see AuthServiceImpl#resendOtp). Telegram is never a
     * registration path of its own: a staff account invited via Telegram
     * (StaffServiceImpl#createViaTelegram) is verified by phone-number match instead of an OTP —
     * see TelegramLinkServiceImpl#verifyPendingContact.
     */
    void resendViaTelegram(String email, String fullName, OtpPurpose purpose, Long chatId);

    void verify(String email, OtpPurpose purpose, String code);
}
