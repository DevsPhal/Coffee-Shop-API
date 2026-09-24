package org.group1.coffeeshopapi.auth.service.impl;

import io.jsonwebtoken.Claims;
import org.group1.coffeeshopapi.auth.dto.request.RefreshTokenRequest;
import org.group1.coffeeshopapi.auth.dto.request.VerifyLoginOtpRequest;
import org.group1.coffeeshopapi.auth.service.OtpService;
import org.group1.coffeeshopapi.auth.service.TokenService;
import org.group1.coffeeshopapi.common.enums.OtpPurpose;
import org.group1.coffeeshopapi.common.enums.UserStatus;
import org.group1.coffeeshopapi.common.exception.InvalidCredentialsException;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Login-ticket and account-status rules around OTP login and token refresh.
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTokenRulesTest {

    @Mock private UserRepository userRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private OtpService otpService;
    @Mock private TokenService tokenService;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthUserSyncService authUserSyncService;
    @Mock private TelegramLinkService telegramLinkService;
    @Spy private SuperAdminProperties superAdminProperties = new SuperAdminProperties();
    @InjectMocks private AuthServiceImpl authService;

    @Test
    void aWrongLoginCodeKeepsTheLoginTicketSoTheUserCanRetry() {
        Customer customer = customer(UserStatus.ACTIVE);
        when(tokenService.peekLoginTicket("ticket")).thenReturn(customer.getId());
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        doThrow(new InvalidOtpException("Invalid verification code"))
                .when(otpService).verify(customer.getEmail(), OtpPurpose.LOGIN, "000000");

        assertThatThrownBy(() -> authService.verifyLoginOtp(new VerifyLoginOtpRequest("ticket", "000000")))
                .isInstanceOf(InvalidOtpException.class);
        verify(tokenService, never()).consumeLoginTicket(anyString());
    }

    @Test
    void theRightCodeUsesUpTheTicket() {
        Customer customer = customer(UserStatus.ACTIVE);
        when(tokenService.peekLoginTicket("ticket")).thenReturn(customer.getId());
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(jwtUtil.generateAccessToken(anyString(), anyString())).thenReturn("access");
        when(jwtUtil.generateRefreshToken(anyString(), anyString())).thenReturn("refresh");

        authService.verifyLoginOtp(new VerifyLoginOtpRequest("ticket", "123456"));

        verify(tokenService).consumeLoginTicket("ticket");
    }

    @Test
    void anAccountDeactivatedMidLoginGetsNoTokens() {
        Customer customer = customer(UserStatus.DEACTIVATED);
        when(tokenService.peekLoginTicket("ticket")).thenReturn(customer.getId());
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> authService.verifyLoginOtp(new VerifyLoginOtpRequest("ticket", "123456")))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(tokenService, never()).storeRefreshToken(any(), anyString());
    }

    @Test
    void aDeactivatedAccountCannotRefreshItsTokens() {
        Customer customer = customer(UserStatus.DEACTIVATED);
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(customer.getEmail());
        when(jwtUtil.extractClaims("refresh")).thenReturn(claims);
        when(jwtUtil.isRefreshToken(claims)).thenReturn(true);
        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> authService.refreshToken(new RefreshTokenRequest("refresh")))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(tokenService, never()).storeRefreshToken(any(), anyString());
    }

    private Customer customer(UserStatus status) {
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setEmail("luku@example.com");
        customer.setStatus(status);
        return customer;
    }
}
