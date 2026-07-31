package org.group1.coffeeshopapi.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.dto.request.LoginRequest;
import org.group1.coffeeshopapi.dto.request.RegisterRequest;
import org.group1.coffeeshopapi.dto.response.*;
import org.group1.coffeeshopapi.entity.Role;
import org.group1.coffeeshopapi.entity.User;
import org.group1.coffeeshopapi.exception.DuplicateResourceException;
import org.group1.coffeeshopapi.exception.InvalidOtpException;
import org.group1.coffeeshopapi.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.repository.UserRepository;
import org.group1.coffeeshopapi.security.JwtService;
import org.group1.coffeeshopapi.service.AuthService;
import org.group1.coffeeshopapi.service.EmailService;
import org.group1.coffeeshopapi.service.OtpService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final OtpService otpService;

    @Override
    public RegisterResponse register(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())){
            throw new DuplicateResourceException("Email already registered");
        }

        User user = User.builder()
                .fullName(registerRequest.getFullName())
                .email(registerRequest.getEmail())
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
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if(!user.isEnabled()){
            throw new DisabledException("Account not verified, Please check your email for OTP.");
        }

        String accessToken = jwtService.generateToken(createUserDetails(user));
        String refreshToken = jwtService.generateToken(createUserDetails(user));
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

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Email not found"));
        if (!otpService.isValid(email, otp)){
            throw new InvalidOtpException("Invalid or expired OTP.");
        }

        user.setEnabled(true);
        userRepository.save(user);

        otpService.clearOtp(email);

        return VerifyOtpResponse.builder()
                .verified(true)
                .email(user.getEmail())
                .build();
    }

    private org.springframework.security.core.userdetails.User createUserDetails(User user) {
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority(
                        "ROLE_" + user.getRole().name()
                ))
        );
    }
}
