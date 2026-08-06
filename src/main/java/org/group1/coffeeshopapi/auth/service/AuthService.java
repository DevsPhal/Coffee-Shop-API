package org.group1.coffeeshopapi.auth.service;

import org.group1.coffeeshopapi.auth.dto.request.LoginRequest;
import org.group1.coffeeshopapi.auth.dto.request.RegisterRequest;
import org.group1.coffeeshopapi.auth.dto.response.LoginResponse;
import org.group1.coffeeshopapi.auth.dto.response.RegisterResponse;
import org.group1.coffeeshopapi.auth.dto.response.VerifyOtpResponse;

public interface AuthService {

    RegisterResponse register(RegisterRequest registerRequest);

    LoginResponse login(LoginRequest loginRequest);

    VerifyOtpResponse verifyOtp(String email, String otp);
}