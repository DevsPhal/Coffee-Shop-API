package org.group1.coffeeshopapi.auth.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.auth.dto.request.ChangePasswordRequest;
import org.group1.coffeeshopapi.auth.dto.request.LoginRequest;
import org.group1.coffeeshopapi.auth.dto.request.RegisterRequest;
import org.group1.coffeeshopapi.auth.dto.request.ResetPasswordRequest;
import org.group1.coffeeshopapi.auth.dto.response.LoginResponse;
import org.group1.coffeeshopapi.auth.dto.response.RegisterResponse;
import org.group1.coffeeshopapi.auth.dto.response.UserResponse;
import org.group1.coffeeshopapi.auth.dto.response.VerifyOtpResponse;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.admin.entity.User;
import org.group1.coffeeshopapi.common.exception.DuplicateResourceException;
import org.group1.coffeeshopapi.common.exception.InvalidOtpException;
import org.group1.coffeeshopapi.common.exception.InvalidRequestException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.common.exception.UnauthorizedException;
import org.group1.coffeeshopapi.admin.repository.UserRepository;
import org.group1.coffeeshopapi.common.security.CustomUserDetails;
import org.group1.coffeeshopapi.common.security.JwtService;
import org.group1.coffeeshopapi.common.security.SecurityUtils;
import org.group1.coffeeshopapi.auth.service.AuthService;
import org.group1.coffeeshopapi.auth.service.EmailService;
import org.group1.coffeeshopapi.auth.service.OtpService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final OtpService otpService;

    @Value("${app.auth.email-required:true}")
    private boolean emailRequired;

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest registerRequest) {
        String clientEmail = normalizeEmail(registerRequest.getEmail());
        if (userRepository.existsByEmail(clientEmail)){
            throw new DuplicateResourceException("Email already registered");
        }

        User user = User.builder()
                .fullName(registerRequest.getFullName().trim())
                .username(clientEmail)
                .email(clientEmail)
                .phoneNumber(registerRequest.getPhone())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .role(Role.CUSTOMER)
                .enabled(false)
                .build();
        userRepository.save(user);

        String otp = sendOtp(user);

        return RegisterResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .verificationRequired(true)
                .devOtp(emailRequired ? null : otp)
                .build();
    }


    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            normalizeEmail(loginRequest.getEmail()),
                            loginRequest.getPassword()
                    )
            );
        }catch (BadCredentialsException ex){
            throw new UnauthorizedException("Invalid email or password.");
        }
        User user = userRepository.findByEmail(normalizeEmail(loginRequest.getEmail()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if(!user.isEnabled()){
            throw new DisabledException("Account not verified, Please check your email for OTP.");
        }

        return buildLoginResponse(user);
    }

    @Override
    public VerifyOtpResponse verifyOtp(String email, String otp) {
        String clientEmail = normalizeEmail(email);

        User user = userRepository.findByEmail(clientEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Email not found"));
        if (!otpService.isValid(clientEmail, otp)){
            throw new InvalidOtpException("Invalid or expired OTP.");
        }

        user.setEnabled(true);
        userRepository.save(user);

        otpService.clearOtp(clientEmail);

        return VerifyOtpResponse.builder()
                .verified(true)
                .email(user.getEmail())
                .build();
    }

    @Override
    public void resendOtp(String email) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("Email not found"));
        if (user.isEnabled()) {
            throw new InvalidRequestException("Account is already verified");
        }
        sendOtp(user);
    }

    @Override
    public LoginResponse refreshToken(String refreshToken) {
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }
        String email = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        if (!user.isEnabled()) {
            throw new DisabledException("Account not verified, Please check your email for OTP.");
        }
        return buildLoginResponse(user);
    }

    @Override
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("Email not found"));
        sendOtp(user);
    }

    @Override
    public VerifyOtpResponse verifyResetOtp(String email, String otp) {
        String clientEmail = normalizeEmail(email);
        userRepository.findByEmail(clientEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Email not found"));
        if (!otpService.isValid(clientEmail, otp)) {
            throw new InvalidOtpException("Invalid or expired OTP.");
        }
        return VerifyOtpResponse.builder()
                .verified(true)
                .email(clientEmail)
                .build();
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        String clientEmail = normalizeEmail(request.getEmail());
        User user = userRepository.findByEmail(clientEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Email not found"));
        if (!otpService.isValid(clientEmail, request.getOtp())) {
            throw new InvalidOtpException("Invalid or expired OTP.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        otpService.clearOtp(clientEmail);
    }

    @Override
    public void changePassword(ChangePasswordRequest request) {
        User user = SecurityUtils.currentUser();
        if (user == null) {
            throw new UnauthorizedException("No authenticated user");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new UnauthorizedException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private String sendOtp(User user) {
        String otp = otpService.generateOtp(user.getEmail());
        emailService.sendOtpEmail(user.getEmail(), user.getFullName(), otp);
        return otp;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private LoginResponse buildLoginResponse(User user) {
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(
                        UserResponse.builder()
                                .id(user.getId())
                                .fullName(user.getFullName())
                                .email(user.getEmail())
                                .role(user.getRole())
                                .build()
                )
                .build();
    }
}
