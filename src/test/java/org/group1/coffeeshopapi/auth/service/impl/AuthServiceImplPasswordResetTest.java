package org.group1.coffeeshopapi.auth.service.impl;

import org.group1.coffeeshopapi.auth.dto.request.ForgotPasswordRequest;
import org.group1.coffeeshopapi.auth.dto.request.ResetPasswordRequest;
import org.group1.coffeeshopapi.auth.service.OtpService;
import org.group1.coffeeshopapi.auth.service.TokenService;
import org.group1.coffeeshopapi.common.enums.OtpPurpose;
import org.group1.coffeeshopapi.common.exception.InvalidOtpException;
import org.group1.coffeeshopapi.common.properties.SuperAdminProperties;
import org.group1.coffeeshopapi.common.util.JwtUtil;
import org.group1.coffeeshopapi.telegram.service.TelegramLinkService;
import org.group1.coffeeshopapi.user.entity.Customer;
import org.group1.coffeeshopapi.user.repository.CustomerRepository;
import org.group1.coffeeshopapi.user.repository.UserRepository;
import org.group1.coffeeshopapi.user.service.AuthUserSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplPasswordResetTest {

    @Mock private UserRepository userRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private OtpService otpService;
    @Mock private TokenService tokenService;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthUserSyncService authUserSyncService;
    @Mock private TelegramLinkService telegramLinkService;
    @Mock private SuperAdminProperties superAdminProperties;
    @InjectMocks private AuthServiceImpl authService;

    @Test
    void requestsAPasswordResetCodeForTheSubmittedAccountEmail() {
        Customer customer = new Customer();
        customer.setFullName("Test Customer");
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customer));

        authService.forgotPassword(new ForgotPasswordRequest("Customer@example.com"));

        verify(otpService).generateAndSend("customer@example.com", "Test Customer", OtpPurpose.RESET_PASSWORD);
        verifyNoInteractions(passwordEncoder, tokenService);
    }

    @Test
    void unknownEmailDoesNotSendACodeOrRevealWhetherAnAccountExists() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        authService.forgotPassword(new ForgotPasswordRequest("unknown@example.com"));

        verifyNoInteractions(otpService);
    }

    @Test
    void acceptedResetCodeUpdatesThePasswordHashAndRevokesRefreshTokens() {
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setPassword("old-password-hash");
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customer));
        when(passwordEncoder.encode("NewPassword!123")).thenReturn("new-password-hash");

        authService.resetPassword(new ResetPasswordRequest("Customer@example.com", "482913", "NewPassword!123"));

        verify(otpService).verify("customer@example.com", OtpPurpose.RESET_PASSWORD, "482913");
        assertThat(customer.getPassword()).isEqualTo("new-password-hash");
        verify(userRepository).save(customer);
        verify(authUserSyncService).sync(customer);
        verify(tokenService).revokeRefreshToken(customer.getId());
    }

    @Test
    void invalidResetCodeCannotChangeThePassword() {
        doThrow(new InvalidOtpException("Invalid verification code"))
                .when(otpService).verify("customer@example.com", OtpPurpose.RESET_PASSWORD, "000000");

        assertThatThrownBy(() -> authService.resetPassword(
                new ResetPasswordRequest("customer@example.com", "000000", "NewPassword!123")))
                .isInstanceOf(InvalidOtpException.class);
        verifyNoInteractions(userRepository, passwordEncoder, authUserSyncService, tokenService);
    }
}
