package org.group1.coffeeshopapi.auth.service.impl;

import org.group1.coffeeshopapi.auth.dto.request.LoginRequest;
import org.group1.coffeeshopapi.auth.dto.response.LoginResponse;
import org.group1.coffeeshopapi.auth.service.OtpService;
import org.group1.coffeeshopapi.auth.service.TokenService;
import org.group1.coffeeshopapi.common.enums.OtpPurpose;
import org.group1.coffeeshopapi.common.properties.SuperAdminProperties;
import org.group1.coffeeshopapi.common.security.SuperAdminUserDetails;
import org.group1.coffeeshopapi.common.util.JwtUtil;
import org.group1.coffeeshopapi.telegram.dto.TelegramLinkCodeResponse;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplLoginTest {

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
    void superAdminLoginSkipsOtpAndRefreshesItsAuthUsersRowOnEveryLogin() {
        superAdminProperties.setEmail("admin@590stcafe.local");
        superAdminProperties.setPassword("admin-password");
        when(jwtUtil.generateAccessToken(anyString(), anyString())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(anyString(), anyString())).thenReturn("refresh-token");
        when(jwtUtil.getAccessExpirationMs()).thenReturn(86_400_000L);

        LoginResponse response = authService.login(new LoginRequest("Admin@590stcafe.local", "admin-password"));

        assertThat(response.otpRequired()).isFalse();
        assertThat(response.tokens().accessToken()).isEqualTo("access-token");
        verify(authUserSyncService).syncSuperAdmin();
        verify(tokenService).storeRefreshToken(SuperAdminUserDetails.ID, "refresh-token");
        verifyNoInteractions(userRepository, otpService);
    }

    @Test
    void regularCustomerLoginChallengesWithOtpAndNeverTouchesTheSuperAdminSync() {
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setEmail("customer@example.com");
        customer.setFullName("Test Customer");
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customer));
        when(tokenService.createLoginTicket(customer.getId())).thenReturn("ticket-123");
        when(telegramLinkService.generateLinkCode(customer.getId()))
                .thenReturn(new TelegramLinkCodeResponse("code", 300, "https://t.me/bot?start=code"));

        LoginResponse response = authService.login(new LoginRequest("customer@example.com", "Password!123"));

        assertThat(response.otpRequired()).isTrue();
        assertThat(response.loginTicket()).isEqualTo("ticket-123");
        verify(otpService).generateAndSend("customer@example.com", "Test Customer", OtpPurpose.LOGIN,
                "https://t.me/bot?start=code");
        verifyNoInteractions(authUserSyncService);
    }
}
