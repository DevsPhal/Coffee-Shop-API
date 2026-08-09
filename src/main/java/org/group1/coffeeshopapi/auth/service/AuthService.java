package org.group1.coffeeshopapi.auth.service;

import org.group1.coffeeshopapi.auth.dto.request.LoginRequest;
import org.group1.coffeeshopapi.auth.dto.request.RegisterRequest;
import org.group1.coffeeshopapi.auth.dto.response.LoginResponse;
import org.group1.coffeeshopapi.auth.dto.response.RefreshTokenResponse;
import org.group1.coffeeshopapi.auth.dto.response.RegisterResponse;
import org.group1.coffeeshopapi.auth.dto.response.VerifyOtpResponse;

public interface AuthService {

    RegisterResponse register(RegisterRequest registerRequest);

    LoginResponse login(LoginRequest loginRequest);

    VerifyOtpResponse verifyOtp(String email, String otp);

    void resendOtp(String email);

    RefreshTokenResponse refreshToken(String refreshToken);

    void forgotPassword(String email);

    VerifyOtpResponse verifyResetOtp(String email, String otp);

    void resetPassword(String email, String otp, String newPassword);

    void changePassword(String oldPassword, String newPassword);
}