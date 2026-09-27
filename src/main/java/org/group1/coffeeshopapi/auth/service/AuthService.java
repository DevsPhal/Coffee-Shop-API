package org.group1.coffeeshopapi.auth.service;

import org.group1.coffeeshopapi.auth.dto.request.*;
import org.group1.coffeeshopapi.auth.dto.response.AuthTokenResponse;
import org.group1.coffeeshopapi.auth.dto.response.LoginResponse;

public interface AuthService {
    void register(RegisterRequest request);

    void verifyRegistration(VerifyRegistrationRequest request);
    LoginResponse login(LoginRequest request);

    // Logs in via the Telegram Login Widget — no email/password. Registers a new customer on the
    // spot if this Telegram id isn't linked to an account yet, otherwise just logs it in.
    AuthTokenResponse loginViaTelegramWidget(TelegramWidgetAuthRequest request);

    // Staff sign-in by phone number: sends a code to the Telegram chat linked to that number and
    // returns a login ticket, finished with verifyLoginOtp exactly like the email flow.
    LoginResponse loginViaPhone(PhoneLoginRequest request);

    AuthTokenResponse verifyLoginOtp(VerifyLoginOtpRequest request);
    void resendOtp(ResendOtpRequest request);
    AuthTokenResponse refreshToken(RefreshTokenRequest request);
    void logout(String accessToken);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
}
