package org.group1.coffeeshopapi.auth.service;

import org.group1.coffeeshopapi.auth.dto.request.*;
import org.group1.coffeeshopapi.auth.dto.response.AuthTokenResponse;
import org.group1.coffeeshopapi.auth.dto.response.LoginResponse;

public interface AuthService {
    void register(RegisterRequest request);

    void verifyRegistration(VerifyRegistrationRequest request);
    LoginResponse login(LoginRequest request);

    /**
     * Authenticates via the official Telegram Login Widget
     * (https://core.telegram.org/widgets/login) — no email/password at all. Telegram itself signs
     * the payload with the bot's secret token, so verifying that signature (see
     * TelegramWidgetAuthVerifier) is proof enough of identity; there's no separate password or OTP
     * step. Register-or-login: if the signed Telegram id is already linked to an account (email
     * {@link #register} + linking Telegram from its profile — see
     * TelegramLinkService#generateLinkCode — or an admin/super-admin-created staff account invited
     * via Telegram — see StaffService#createViaTelegram), this just logs it in; otherwise a brand
     * new Customer is created on the spot, active immediately. Always Role.CUSTOMER when creating
     * — there's no way to request ADMIN/BARISTA this way, and never should be; those stay
     * invite-only.
     */
    AuthTokenResponse loginViaTelegramWidget(TelegramWidgetAuthRequest request);

    AuthTokenResponse verifyLoginOtp(VerifyLoginOtpRequest request);
    void resendOtp(ResendOtpRequest request);
    AuthTokenResponse refreshToken(RefreshTokenRequest request);
    void logout(String accessToken);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
}
