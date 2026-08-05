package org.group1.coffeeshopapi.service;

import org.group1.coffeeshopapi.dto.request.LoginRequest;
import org.group1.coffeeshopapi.dto.request.RegisterRequest;
import org.group1.coffeeshopapi.dto.response.LoginResponse;
import org.group1.coffeeshopapi.dto.response.RegisterResponse;
import org.group1.coffeeshopapi.dto.response.VerifyOtpResponse;

public interface AuthService {

    RegisterResponse register(RegisterRequest registerRequest);

    LoginResponse login(LoginRequest loginRequest);

    VerifyOtpResponse verifyOtp(String email, String otp);
}