package org.group1.coffeeshopapi.auth.service.impl;

import org.group1.coffeeshopapi.auth.dto.request.RegisterRequest;
import org.group1.coffeeshopapi.auth.service.OtpService;
import org.group1.coffeeshopapi.auth.service.TokenService;
import org.group1.coffeeshopapi.common.enums.Gender;
import org.group1.coffeeshopapi.common.enums.OtpPurpose;
import org.group1.coffeeshopapi.common.enums.UserStatus;
import org.group1.coffeeshopapi.common.exception.DuplicateResourceException;
import org.group1.coffeeshopapi.common.properties.SuperAdminProperties;
import org.group1.coffeeshopapi.common.util.JwtUtil;
import org.group1.coffeeshopapi.telegram.service.TelegramLinkService;
import org.group1.coffeeshopapi.user.entity.Customer;
import org.group1.coffeeshopapi.user.repository.CustomerRepository;
import org.group1.coffeeshopapi.user.repository.UserRepository;
import org.group1.coffeeshopapi.user.service.AuthUserSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplRegistrationTest {

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

    private final RegisterRequest request = new RegisterRequest(
            "Test Customer", "Customer@example.com", "Example!123", "0963001940", Gender.MALE);

    @Test
    void explainsWhyTheReservedAdminAddressCannotReceiveARegistrationOtp() {
        superAdminProperties.setEmail("customer@example.com");
        superAdminProperties.setPassword("admin-password");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("reserved for the administrator");
        verifyNoInteractions(customerRepository, otpService);
    }

    @Test
    void sendsTheRegistrationOtpToTheFormEmailWhenAdminUsesASeparateAddress() {
        superAdminProperties.setEmail("superadmin@590stcafe.local");
        superAdminProperties.setPassword("admin-password");
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");

        authService.register(request);

        ArgumentCaptor<Customer> saved = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("customer@example.com");
        assertThat(saved.getValue().getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);
        verify(otpService).generateAndSend("customer@example.com", "Test Customer", OtpPurpose.REGISTER);
    }
}
