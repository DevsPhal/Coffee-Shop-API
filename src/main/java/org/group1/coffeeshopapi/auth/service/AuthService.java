package org.group1.coffeeshopapi.auth.service;

import org.group1.coffeeshopapi.auth.dto.request.*;
import org.group1.coffeeshopapi.auth.dto.response.AuthTokenResponse;
import org.group1.coffeeshopapi.auth.dto.response.LoginResponse;

public interface AuthService {
    void register(RegisterRequest request);
    void verifyRegistration(VerifyRegistrationRequest request);
    LoginResponse login(LoginRequest request);
    AuthTokenResponse verifyLoginOtp(VerifyLoginOtpRequest request);
    void resendOtp(ResendOtpRequest request);
    AuthTokenResponse refreshToken(RefreshTokenRequest request);
    void logout(String accessToken);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
}