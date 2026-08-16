package org.group1.coffeeshopapi.auth.service.impl;

import io.jsonwebtoken.JwtException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group1.coffeeshopapi.auth.dto.request.LoginRequest;
import org.group1.coffeeshopapi.auth.dto.request.RegisterRequest;
import org.group1.coffeeshopapi.auth.dto.response.LoginResponse;
import org.group1.coffeeshopapi.auth.dto.response.RefreshTokenResponse;
import org.group1.coffeeshopapi.auth.dto.response.RegisterResponse;
import org.group1.coffeeshopapi.auth.dto.response.UserResponse;
import org.group1.coffeeshopapi.auth.dto.response.VerifyOtpResponse;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.user.entity.User;
import org.group1.coffeeshopapi.common.exception.DuplicateResourceException;
import org.group1.coffeeshopapi.common.exception.InvalidOtpException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.common.exception.UnauthorizedException;
import org.group1.coffeeshopapi.user.repository.UserRepository;
import org.group1.coffeeshopapi.common.security.CustomUserDetails;
import org.group1.coffeeshopapi.common.security.JwtService;
import org.group1.coffeeshopapi.auth.service.AuthService;
import org.group1.coffeeshopapi.auth.service.EmailService;
import org.group1.coffeeshopapi.auth.service.OtpService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final OtpService otpService;

    @Override
    public RegisterResponse register(RegisterRequest registerRequest) {
        String email = normalizeEmail(registerRequest.getEmail());
        if (userRepository.existsByEmail(email)){
            throw new DuplicateResourceException("Email already registered");
        }

        User user = User.builder()
                .fullName(registerRequest.getFullName())
                .email(email)
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .role(Role.CUSTOMER)
                .enabled(false)
                .build();
        userRepository.save(user);

        String otp = otpService.generateOtp(user.getEmail());

        emailService.sendOtpEmail(
                user.getEmail(),
                user.getFullName(),
                otp
        );
        return RegisterResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .verificationRequired(true)
                .build();
    }


    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        String email = normalizeEmail(loginRequest.getEmail());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            email,
                            loginRequest.getPassword()
                    )
            );
        } catch (BadCredentialsException ex) {
            throw new UnauthorizedException("Invalid email or password.");
        } catch (DisabledException ex) {
            throw new DisabledException("Account not verified, Please check your email for OTP.");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        CustomUserDetails userDetails = new CustomUserDetails(user);

        String accessToken = jwtService.generateAccessToken(userDetails);
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

    @Override
    public VerifyOtpResponse verifyOtp(String email, String otp) {
        String normalizedEmail = normalizeEmail(email);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Email not found"));
        if (!otpService.isValid(normalizedEmail, otp)){
            throw new InvalidOtpException("Invalid or expired OTP.");
        }

        user.setEnabled(true);
        userRepository.save(user);

        otpService.clearOtp(normalizedEmail);

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
            throw new DuplicateResourceException("Account is already verified, please login.");
        }

        String otp = otpService.generateOtp(user.getEmail());
        emailService.sendOtpEmail(user.getEmail(), user.getFullName(), otp);
    }

    @Override
    public RefreshTokenResponse refreshToken(String refreshToken) {
        try {
            String email = jwtService.extractUsername(refreshToken);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token."));
            CustomUserDetails userDetails = new CustomUserDetails(user);

            if (!jwtService.isRefreshTokenValid(refreshToken, userDetails)) {
                throw new UnauthorizedException("Invalid or expired refresh token.");
            }

            String accessToken = jwtService.generateAccessToken(userDetails);
            return RefreshTokenResponse.builder()
                    .accessToken(accessToken)
                    .build();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new UnauthorizedException("Invalid or expired refresh token.");
        }
    }

    @Override
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("Email not found"));

        String otp = otpService.generateOtp(user.getEmail());
        emailService.sendOtpEmail(user.getEmail(), user.getFullName(), otp);
    }

    @Override
    public VerifyOtpResponse verifyResetOtp(String email, String otp) {
        String normalizedEmail = normalizeEmail(email);
        userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Email not found"));

        if (!otpService.isValid(normalizedEmail, otp)) {
            throw new InvalidOtpException("Invalid or expired OTP.");
        }

        return VerifyOtpResponse.builder()
                .verified(true)
                .email(normalizedEmail)
                .build();
    }

    @Override
    public void resetPassword(String email, String otp, String newPassword) {
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Email not found"));

        if (!otpService.isValid(normalizedEmail, otp)) {
            throw new InvalidOtpException("Invalid or expired OTP.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        otpService.clearOtp(normalizedEmail);
    }

    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    @Override
    public void changePassword(String oldPassword, String newPassword) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new UnauthorizedException("Old password is incorrect.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}