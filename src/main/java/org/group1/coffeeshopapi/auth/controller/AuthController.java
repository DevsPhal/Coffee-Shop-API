package org.group1.coffeeshopapi.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.auth.dto.request.*;
import org.group1.coffeeshopapi.auth.dto.response.LoginResponse;
import org.group1.coffeeshopapi.auth.dto.response.RegisterResponse;
import org.group1.coffeeshopapi.auth.dto.response.UserResponse;
import org.group1.coffeeshopapi.auth.dto.response.VerifyOtpResponse;
import org.group1.coffeeshopapi.common.responses.ApiResponse;
import org.group1.coffeeshopapi.auth.service.AuthService;
import org.group1.coffeeshopapi.admin.service.UserService;
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
        authService.resendOtp(emailRequest.getEmail());
        return ResponseEntity.ok(ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("OTP resent. Please check your email.")
                .timeStamp(LocalDateTime.now())
                .build());
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
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest){
        LoginResponse response = authService.refreshToken(refreshTokenRequest.getToken());
        return ResponseEntity.ok(ApiResponse.<LoginResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Token refreshed successfully")
                .data(response)
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<?>> logout(HttpServletRequest request){
        // Stateless JWT: there is no server-side session or token store to clear here - the
        // client is responsible for discarding its access/refresh tokens on logout.
        return ResponseEntity.ok(ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Logged out successfully")
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<?>> forgotPassword(@Valid @RequestBody EmailRequest emailRequest){
        authService.forgotPassword(emailRequest.getEmail());
        return ResponseEntity.ok(ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Password reset OTP sent. Please check your email.")
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/verify-reset-otp")
    public ResponseEntity<ApiResponse<VerifyOtpResponse>> verifyResetOtp(@Valid @RequestBody VerifyOtpRequest request){
        VerifyOtpResponse response = authService.verifyResetOtp(request.getEmail(), request.getOtp());
        return ResponseEntity.ok(ApiResponse.<VerifyOtpResponse>builder()
                .status(HttpStatus.OK.value())
                .message("OTP verified")
                .data(response)
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<?>> resetPassword(@Valid @RequestBody ResetPasswordRequest request){
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Password reset successfully. Please login.")
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<?>> changePassword(@Valid @RequestBody ChangePasswordRequest request){
        authService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Password changed successfully")
                .timeStamp(LocalDateTime.now())
                .build());
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