package org.group1.coffeeshopapi.auth.service.impl;

import org.group1.coffeeshopapi.admin.repository.UserRepository;
import org.group1.coffeeshopapi.auth.dto.request.RegisterRequest;
import org.group1.coffeeshopapi.auth.service.EmailService;
import org.group1.coffeeshopapi.auth.service.OtpService;
import org.group1.coffeeshopapi.common.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {

    @Test
    void registerSendsOtpToRegisteredClientEmail() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        EmailService emailService = mock(EmailService.class);
        OtpService otpService = mock(OtpService.class);
        AuthServiceImpl authService = new AuthServiceImpl(
                userRepository,
                passwordEncoder,
                jwtService,
                authenticationManager,
                emailService,
                otpService
        );

        RegisterRequest request = new RegisterRequest();
        request.setFullName("Visal Soeurn");
        request.setEmail(" VISALSOEURN9@gmail.com ");
        request.setPhone("0963001940");
        request.setPassword("Qwert12!@");

        when(userRepository.existsByEmail("visalsoeurn9@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode("Qwert12!@")).thenReturn("hashed-password");
        when(otpService.generateOtp("visalsoeurn9@gmail.com")).thenReturn("123456");

        authService.register(request);

        verify(otpService).generateOtp("visalsoeurn9@gmail.com");
        verify(emailService).sendOtpEmail("visalsoeurn9@gmail.com", "Visal Soeurn", "123456");
    }
}
