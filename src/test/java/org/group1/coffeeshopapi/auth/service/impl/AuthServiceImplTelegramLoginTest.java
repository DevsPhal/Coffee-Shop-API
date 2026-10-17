package org.group1.coffeeshopapi.auth.service.impl;

import org.group1.coffeeshopapi.admin.entity.Admin;
import org.group1.coffeeshopapi.admin.repository.AdminRepository;
import org.group1.coffeeshopapi.auth.dto.request.LoginRequest;
import org.group1.coffeeshopapi.auth.dto.request.PhoneLoginRequest;
import org.group1.coffeeshopapi.auth.dto.request.TelegramLoginStatusRequest;
import org.group1.coffeeshopapi.auth.dto.response.LoginResponse;
import org.group1.coffeeshopapi.auth.dto.response.TelegramLoginStatusResponse;
import org.group1.coffeeshopapi.auth.service.OtpService;
import org.group1.coffeeshopapi.auth.service.TokenService;
import org.group1.coffeeshopapi.barista.repository.BaristaRepository;
import org.group1.coffeeshopapi.common.enums.OtpPurpose;
import org.group1.coffeeshopapi.common.enums.RegisterType;
import org.group1.coffeeshopapi.common.enums.TelegramLoginStatus;
import org.group1.coffeeshopapi.common.enums.UserStatus;
import org.group1.coffeeshopapi.common.exception.InvalidCredentialsException;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.properties.SuperAdminProperties;
import org.group1.coffeeshopapi.common.util.JwtUtil;
import org.group1.coffeeshopapi.telegram.dto.TelegramLoginChallenge;
import org.group1.coffeeshopapi.telegram.dto.TelegramLoginOutcome;
import org.group1.coffeeshopapi.telegram.dto.TelegramUser;
import org.group1.coffeeshopapi.telegram.exception.TelegramDeliveryException;
import org.group1.coffeeshopapi.telegram.service.TelegramApiClient;
import org.group1.coffeeshopapi.telegram.service.TelegramLinkService;
import org.group1.coffeeshopapi.telegram.service.TelegramLoginService;
import org.group1.coffeeshopapi.telegram.service.TelegramProfileSyncService;
import org.group1.coffeeshopapi.telegram.config.TelegramProperties;
import org.group1.coffeeshopapi.auth.dto.request.TelegramWidgetAuthRequest;
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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTelegramLoginTest {

    private static final TelegramLoginStatusRequest POLL = new TelegramLoginStatusRequest("request-id", "poll-token");
    private static final String BOT_TOKEN = "123456:SECRET-bot-token";
    private static final String PHOTO_URL = "https://t.me/i/userpic/320/alice.jpg";

    @Mock private UserRepository userRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private AdminRepository adminRepository;
    @Mock private BaristaRepository baristaRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private OtpService otpService;
    @Mock private TokenService tokenService;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthUserSyncService authUserSyncService;
    @Mock private TelegramLinkService telegramLinkService;
    @Mock private TelegramApiClient telegramApiClient;
    @Mock private TelegramLoginService telegramLoginService;
    @Mock private TelegramProfileSyncService telegramProfileSyncService;
    @Spy private SuperAdminProperties superAdminProperties = new SuperAdminProperties();
    @Spy private TelegramProperties telegramProperties = telegramProperties();
    @InjectMocks private AuthServiceImpl authService;

    @Test
    void startingATelegramLoginHandsBackTheChallenge() {
        TelegramLoginChallenge challenge = new TelegramLoginChallenge("id", "token", "https://t.me/CafeBot?start=login_id",
                "CafeBot", 300);
        when(telegramLoginService.createChallenge()).thenReturn(challenge);

        assertThat(authService.startTelegramLogin()).isEqualTo(challenge);
    }

    @Test
    void startingFailsSafelyWithAClearMessageWhenTelegramIsNotConfigured() {
        when(telegramLoginService.createChallenge()).thenThrow(new TelegramDeliveryException(
                TelegramDeliveryException.Reason.NOT_CONFIGURED, "Telegram sign-in isn't available right now."));

        assertThatThrownBy(() -> authService.startTelegramLogin())
                .isInstanceOf(TelegramDeliveryException.class)
                .hasMessageContaining("isn't available");
    }

    @Test
    void anUnfinishedRequestReportsItsStatusWithoutIssuingTokens() {
        for (TelegramLoginStatus status : List.of(TelegramLoginStatus.PENDING, TelegramLoginStatus.AWAITING_CONFIRMATION,
                TelegramLoginStatus.REJECTED, TelegramLoginStatus.EXPIRED)) {
            when(telegramLoginService.checkChallenge("request-id", "poll-token"))
                    .thenReturn(TelegramLoginOutcome.of(status));

            TelegramLoginStatusResponse response = authService.checkTelegramLogin(POLL);

            assertThat(response.status()).isEqualTo(status);
            assertThat(response.tokens()).isNull();
        }
        verifyNoInteractions(userRepository, customerRepository, jwtUtil);
    }

    @Test
    void anApprovedTelegramAccountThatIsAlreadyLinkedSignsIntoThatAccountWithoutCreatingAnother() {
        Customer existing = customer(UUID.randomUUID(), "alice@example.com", UserStatus.ACTIVE);
        approved(new TelegramUser(9001L, "Alice", "Smith", "alice"));
        when(userRepository.findByTelegramChatId("9001")).thenReturn(Optional.of(existing));
        stubTokens();

        TelegramLoginStatusResponse response = authService.checkTelegramLogin(POLL);

        assertThat(response.status()).isEqualTo(TelegramLoginStatus.APPROVED);
        assertThat(response.tokens().accessToken()).isEqualTo("access-token");
        verify(jwtUtil).generateAccessToken("alice@example.com", "CUSTOMER");
        verify(tokenService).storeRefreshToken(existing.getId(), "refresh-token");
        verify(customerRepository, never()).saveAndFlush(any());
        verify(telegramProfileSyncService).sync(existing, 9001L, "alice", null);
    }

    @Test
    void anApprovedTelegramAccountWithNoLinkedAccountGetsANewCustomerAccount() {
        approved(new TelegramUser(9001L, "Alice", "Smith", "alice"));
        when(userRepository.findByTelegramChatId("9001")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("random-hash");
        stubTokens();

        TelegramLoginStatusResponse response = authService.checkTelegramLogin(POLL);

        ArgumentCaptor<Customer> saved = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getTelegramChatId()).isEqualTo("9001");
        assertThat(saved.getValue().getTelegramUsername()).isEqualTo("alice");
        assertThat(saved.getValue().getFullName()).isEqualTo("Alice Smith");
        assertThat(saved.getValue().getRegisterType()).isEqualTo(RegisterType.TELEGRAM);
        assertThat(saved.getValue().getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(response.tokens()).isNotNull();
        verify(telegramApiClient).sendHtmlMessageWithButtons(eq(9001L), anyString());
        verify(telegramProfileSyncService).sync(saved.getValue(), 9001L, "alice", null);
    }

    @Test
    void aSignedWidgetLoginSyncsTheProfilePhotoFromTheSignedPhotoUrl() {
        Customer existing = customer(UUID.randomUUID(), "alice@example.com", UserStatus.ACTIVE);
        when(userRepository.findByTelegramChatId("9001")).thenReturn(Optional.of(existing));
        stubTokens();

        authService.loginViaTelegramWidget(signedWidgetLogin(PHOTO_URL));

        verify(telegramProfileSyncService).sync(existing, 9001L, "alice", PHOTO_URL);
    }

    @Test
    void aNewWidgetAccountNoLongerHotlinksTheTelegramPhotoUrl() {
        when(userRepository.findByTelegramChatId("9001")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("random-hash");
        stubTokens();

        authService.loginViaTelegramWidget(signedWidgetLogin(PHOTO_URL));

        ArgumentCaptor<Customer> saved = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getAvatarUrl()).isNull();
        verify(telegramProfileSyncService).sync(saved.getValue(), 9001L, "alice", PHOTO_URL);
    }

    @Test
    void aWidgetLoginWithATamperedPhotoUrlIsRejectedBeforeAnythingIsFetched() {
        TelegramWidgetAuthRequest signed = signedWidgetLogin(PHOTO_URL);
        TelegramWidgetAuthRequest tampered = new TelegramWidgetAuthRequest(signed.id(), signed.firstName(),
                signed.lastName(), signed.username(), "https://attacker.example/x.jpg", signed.authDate(), signed.hash());

        assertThatThrownBy(() -> authService.loginViaTelegramWidget(tampered))
                .isInstanceOf(InvalidCredentialsException.class);
        verifyNoInteractions(telegramProfileSyncService, userRepository, jwtUtil);
    }

    @Test
    void anInactiveAccountIsRefusedWithoutSyncingItsProfile() {
        when(userRepository.findByTelegramChatId("9001"))
                .thenReturn(Optional.of(customer(UUID.randomUUID(), "alice@example.com", UserStatus.SUSPENDED)));

        assertThatThrownBy(() -> authService.loginViaTelegramWidget(signedWidgetLogin(PHOTO_URL)))
                .isInstanceOf(InvalidCredentialsException.class);
        verifyNoInteractions(telegramProfileSyncService);
    }

    @Test
    void anApprovedTelegramAccountWithoutAUsernameCannotCreateAnAccount() {
        approved(new TelegramUser(9001L, "Alice", null, null));
        when(userRepository.findByTelegramChatId("9001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.checkTelegramLogin(POLL))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("username");
        verify(customerRepository, never()).saveAndFlush(any());
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void aLinkedAccountThatIsNotActiveIsRefusedEvenAfterTelegramApproval() {
        approved(new TelegramUser(9001L, "Alice", "Smith", "alice"));
        when(userRepository.findByTelegramChatId("9001"))
                .thenReturn(Optional.of(customer(UUID.randomUUID(), "alice@example.com", UserStatus.SUSPENDED)));

        assertThatThrownBy(() -> authService.checkTelegramLogin(POLL))
                .isInstanceOf(InvalidCredentialsException.class);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void staffPhoneLoginReportsAFailedTelegramDeliveryAndHandsOutNoLoginTicket() {
        Admin admin = admin("+85512345678", "555");
        when(adminRepository.findAll()).thenReturn(List.of(admin));
        doThrow(new TelegramDeliveryException(TelegramDeliveryException.Reason.CHAT_UNREACHABLE,
                "Open @CafeBot in Telegram, tap Start"))
                .when(otpService).generateAndSendViaTelegram(admin.getEmail(), admin.getFullName(), OtpPurpose.LOGIN, 555L);

        assertThatThrownBy(() -> authService.loginViaPhone(new PhoneLoginRequest("012 345 678")))
                .isInstanceOf(TelegramDeliveryException.class)
                .hasMessageContaining("tap Start");
        verify(tokenService, never()).createLoginTicket(any(), anyBoolean());
    }

    @Test
    void staffPhoneLoginSendsTheCodeToTheLinkedChatAndReturnsATelegramTicket() {
        Admin admin = admin("+85512345678", "555");
        when(adminRepository.findAll()).thenReturn(List.of(admin));
        when(tokenService.createLoginTicket(admin.getId(), true)).thenReturn("ticket-1");

        LoginResponse response = authService.loginViaPhone(new PhoneLoginRequest("012 345 678"));

        assertThat(response.otpRequired()).isTrue();
        assertThat(response.loginTicket()).isEqualTo("ticket-1");
        verify(otpService).generateAndSendViaTelegram(admin.getEmail(), admin.getFullName(), OtpPurpose.LOGIN, 555L);
    }

    @Test
    void emailAndPasswordLoginIsUnaffectedAndStillChallengesWithAnEmailCode() {
        Customer customer = customer(UUID.randomUUID(), "customer@example.com", UserStatus.ACTIVE);
        customer.setTelegramChatId("9001");
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customer));
        when(tokenService.createLoginTicket(customer.getId())).thenReturn("ticket-2");

        LoginResponse response = authService.login(new LoginRequest("Customer@Example.com", "Password!123"));

        assertThat(response.otpRequired()).isTrue();
        assertThat(response.loginTicket()).isEqualTo("ticket-2");
        verify(otpService).generateAndSend("customer@example.com", customer.getFullName(), OtpPurpose.LOGIN, null);
        verifyNoInteractions(telegramLoginService);
        verify(telegramApiClient, never()).sendHtmlMessageOrThrow(anyLong(), anyString());
    }

    private void approved(TelegramUser telegramUser) {
        when(telegramLoginService.checkChallenge("request-id", "poll-token"))
                .thenReturn(TelegramLoginOutcome.approved(telegramUser));
    }

    private void stubTokens() {
        when(jwtUtil.generateAccessToken(anyString(), anyString())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(anyString(), anyString())).thenReturn("refresh-token");
        when(jwtUtil.getAccessExpirationMs()).thenReturn(86_400_000L);
    }

    private static TelegramProperties telegramProperties() {
        TelegramProperties properties = new TelegramProperties();
        properties.setBotToken(BOT_TOKEN);
        return properties;
    }

    // Signs the payload the way Telegram's Login Widget does: HMAC-SHA256 keyed by SHA-256(bot token).
    private static TelegramWidgetAuthRequest signedWidgetLogin(String photoUrl) {
        long authDate = Instant.now().getEpochSecond();
        String dataCheckString = String.join("\n", List.of("auth_date=" + authDate, "first_name=Alice", "id=9001",
                "last_name=Smith", "photo_url=" + photoUrl, "username=alice"));
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(MessageDigest.getInstance("SHA-256")
                    .digest(BOT_TOKEN.getBytes(StandardCharsets.UTF_8)), "HmacSHA256"));
            String hash = HexFormat.of().formatHex(mac.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8)));
            return new TelegramWidgetAuthRequest(9001L, "Alice", "Smith", "alice", photoUrl, authDate, hash);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static Customer customer(UUID id, String email, UserStatus status) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setEmail(email);
        customer.setFullName("Alice Smith");
        customer.setStatus(status);
        return customer;
    }

    private static Admin admin(String phoneNumber, String telegramChatId) {
        Admin admin = new Admin();
        admin.setId(UUID.randomUUID());
        admin.setEmail("telegram+staff@telegram.internal");
        admin.setFullName("Staff Member");
        admin.setPhoneNumber(phoneNumber);
        admin.setTelegramChatId(telegramChatId);
        admin.setStatus(UserStatus.ACTIVE);
        return admin;
    }
}
