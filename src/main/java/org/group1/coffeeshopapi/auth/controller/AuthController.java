package org.group1.coffeeshopapi.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.auth.dto.request.*;
import org.group1.coffeeshopapi.auth.dto.response.LoginResponse;
import org.group1.coffeeshopapi.auth.dto.response.RefreshTokenResponse;
import org.group1.coffeeshopapi.auth.dto.response.RegisterResponse;
import org.group1.coffeeshopapi.auth.dto.response.VerifyOtpResponse;
import org.group1.coffeeshopapi.auth.service.AuthService;
import org.group1.coffeeshopapi.user.dto.response.UserResponse;
import org.group1.coffeeshopapi.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/register")
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/verify-otp")
    public VerifyOtpResponse verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return authService.verifyOtp(request.getEmail(), request.getOtp());
    }

    @PostMapping("/resend-otp")
    public void resendOtp(@Valid @RequestBody EmailRequest emailRequest){
        authService.resendOtp(emailRequest.getEmail());
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request){
        return authService.login(request);
    }

    @PostMapping("/refresh-token")
    public RefreshTokenResponse refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest){
        return authService.refreshToken(refreshTokenRequest.getToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest httpRequest,
            @RequestBody(required = false) RefreshTokenRequest refreshTokenRequest){
        String authHeader = httpRequest.getHeader("Authorization");
        String accessToken = (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7) : null;
        String refreshToken = refreshTokenRequest != null ? refreshTokenRequest.getToken() : null;
        authService.logout(accessToken, refreshToken);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Logged out successfully.")
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/forgot-password")
    public void forgotPassword(@Valid @RequestBody EmailRequest emailRequest){
        authService.forgotPassword(emailRequest.getEmail());
    }

    @PostMapping("/verify-reset-otp")
    public VerifyOtpResponse verifyResetOtp(@Valid @RequestBody VerifyOtpRequest request){
        return authService.verifyResetOtp(request.getEmail(), request.getOtp());
    }

    @PostMapping("/reset-password")
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request){
        authService.resetPassword(request.getEmail(), request.getOtp(), request.getNewPassword());
    }

    @PostMapping("/change-password")
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request){
        authService.changePassword(request.getOldPassword(), request.getNewPassword());
    }

    @GetMapping("/me")
    public UserResponse me(){
        return userService.getCurrentUser();
    }
}
