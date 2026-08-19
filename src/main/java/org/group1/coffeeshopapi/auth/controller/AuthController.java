package org.group1.coffeeshopapi.auth.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.auth.dto.request.*;
import org.group1.coffeeshopapi.auth.dto.response.AuthTokenResponse;
import org.group1.coffeeshopapi.auth.dto.response.LoginResponse;
import org.group1.coffeeshopapi.auth.service.AuthService;
import org.group1.coffeeshopapi.common.constant.AppConstant;
import org.group1.coffeeshopapi.common.constant.SecurityConstants;
import org.group1.coffeeshopapi.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Registration, password + OTP two-factor login, tokens, and password reset")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(HttpStatus.CREATED,
                "Registration successful. Please check your email for a verification code.", null));
    }

    @PostMapping("/verify-registration")
    public ResponseEntity<ApiResponse<Void>> verifyRegistration(@Valid @RequestBody VerifyRegistrationRequest request) {
        authService.verifyRegistration(request);
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK,
                "Account verified successfully. You can now log in.", null));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse result = authService.login(request);
        String message = result.otpRequired()
                ? "A verification code has been sent to your email."
                : "Login successful.";
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, message, result));
    }

    @PostMapping("/verify-login-otp")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> verifyLoginOtp(@Valid @RequestBody VerifyLoginOtpRequest request) {
        AuthTokenResponse tokens = authService.verifyLoginOtp(request);
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "Login successful.", tokens));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<Void>> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        authService.resendOtp(request);
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK,
                "If eligible, a new verification code has been sent.", null));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthTokenResponse tokens = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, tokens));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = SecurityConstants.JWT_HEADER, required = false) String authorizationHeader) {
        String accessToken = authorizationHeader != null && authorizationHeader.startsWith(SecurityConstants.JWT_PREFIX)
                ? authorizationHeader.substring(SecurityConstants.JWT_PREFIX.length())
                : "";
        authService.logout(accessToken);
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "Logged out successfully", null));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK,
                "If an account exists for this email, a reset code has been sent.", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK,
                "Password reset successfully. Please log in with your new password.", null));
    }
}