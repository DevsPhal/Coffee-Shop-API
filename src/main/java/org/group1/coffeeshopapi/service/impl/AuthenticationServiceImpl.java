package org.group1.coffeeshopapi.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.dto.request.LoginRequest;
import org.group1.coffeeshopapi.dto.request.RegisterRequest;
import org.group1.coffeeshopapi.dto.response.AuthenticationResponse;
import org.group1.coffeeshopapi.entity.Role;
import org.group1.coffeeshopapi.entity.User;
import org.group1.coffeeshopapi.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.repository.UserRepository;
import org.group1.coffeeshopapi.security.JwtService;
import org.group1.coffeeshopapi.service.AuthenticationService;
import org.group1.coffeeshopapi.service.EmailService;
import org.group1.coffeeshopapi.service.OtpService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final OtpService otpService;
    @Override
    public AuthenticationResponse register(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())){
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .fullName(registerRequest.getFullName())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .role(Role.CUSTOMER)
                .enabled(false)
                .build();
        userRepository.save(user);

        String otp = otpService.generateOtp();

        emailService.sendOtpEmail(
                user.getEmail(),
                user.getFullName(),
                otp
        );
        return AuthenticationResponse.builder()
                .message("Register success, please check your email for OTP.")
                .build();
    }

    @Override
    public AuthenticationResponse login(LoginRequest loginRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if(!user.isEnabled()){
            throw new RuntimeException("Account not verified");
        }

        String token = jwtService.generateToken(createUserDetails(user));
        return AuthenticationResponse.builder()
                .token(token)
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
