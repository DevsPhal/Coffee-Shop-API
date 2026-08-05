package org.group1.coffeeshopapi.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.dto.common.ApiResponse;
import org.group1.coffeeshopapi.dto.request.*;
import org.group1.coffeeshopapi.dto.response.*;
import org.group1.coffeeshopapi.service.AuthService;
import org.group1.coffeeshopapi.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<RegisterResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Register successful. Please check your email for OTP.")
                .data(response)
                .timeStamp(LocalDateTime.now())
                .build());

    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<VerifyOtpResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        VerifyOtpResponse response = authService.verifyOtp(request.getEmail(), request.getOtp());
        return ResponseEntity.ok(ApiResponse.<VerifyOtpResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Account verified successfully, Please login.")
                .data(response)
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<?>> resendOtp(@Valid @RequestBody EmailRequest emailRequest){
        return null;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request){
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.<LoginResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Login successfully")
                .data(response)
                .timeStamp(LocalDateTime.now())
                .build()
        );
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<?>> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest){
        return null;
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<?>> logout(HttpServletRequest request){
        return null;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<?>> forgotPassword(@Valid @RequestBody EmailRequest emailRequest){
        return null;
    }

    @PostMapping("/verify-reset-otp")
    public ResponseEntity<ApiResponse<?>> verifyResetOtp(@Valid @RequestBody VerifyOtpRequest request){
        return null;
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<?>> resetPassword(@Valid @RequestBody ResetPasswordRequest request){
        return null;
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<?>> changePassword(@Valid @RequestBody ChangePasswordRequest request){
        return null;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(){
        UserResponse response = userService.getCurrentUser();
        return ResponseEntity.ok(
                ApiResponse.<UserResponse>builder()
                        .status(HttpStatus.OK.value())
                        .message("Get current user successfully.")
                        .data(response)
                        .timeStamp(LocalDateTime.now())
                        .build()
        );
    }
}