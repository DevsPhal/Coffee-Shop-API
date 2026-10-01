package org.group1.coffeeshopapi.auth.service.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.auth.dto.request.*;
import org.group1.coffeeshopapi.auth.dto.response.AuthTokenResponse;
import org.group1.coffeeshopapi.auth.dto.response.LoginResponse;
import org.group1.coffeeshopapi.auth.service.AuthService;
import org.group1.coffeeshopapi.auth.service.OtpService;
import org.group1.coffeeshopapi.auth.service.TokenService;
import org.group1.coffeeshopapi.common.enums.OtpPurpose;
import org.group1.coffeeshopapi.common.enums.RegisterType;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.enums.UserStatus;
import org.group1.coffeeshopapi.common.exception.DuplicateResourceException;
import org.group1.coffeeshopapi.common.exception.InvalidCredentialsException;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.common.properties.SuperAdminProperties;
import org.group1.coffeeshopapi.common.security.SuperAdminUserDetails;
import org.group1.coffeeshopapi.common.util.JwtUtil;
import org.group1.coffeeshopapi.common.util.PhoneNumberUtil;
import org.group1.coffeeshopapi.admin.repository.AdminRepository;
import org.group1.coffeeshopapi.barista.repository.BaristaRepository;
import org.group1.coffeeshopapi.telegram.config.TelegramProperties;
import org.group1.coffeeshopapi.telegram.service.TelegramApiClient;
import org.group1.coffeeshopapi.telegram.service.TelegramLinkService;
import org.group1.coffeeshopapi.telegram.util.TelegramAccountUtil;
import org.group1.coffeeshopapi.telegram.util.TelegramFormat;
import org.group1.coffeeshopapi.telegram.util.TelegramWidgetAuthVerifier;
import org.group1.coffeeshopapi.user.entity.Customer;
import org.group1.coffeeshopapi.user.entity.User;
import org.group1.coffeeshopapi.user.repository.CustomerRepository;
import org.group1.coffeeshopapi.user.repository.UserRepository;
import org.group1.coffeeshopapi.user.service.AuthUserSyncService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final AdminRepository adminRepository;
    private final BaristaRepository baristaRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final OtpService otpService;
    private final TokenService tokenService;
    private final JwtUtil jwtUtil;
    private final SuperAdminProperties superAdminProperties;
    private final AuthUserSyncService authUserSyncService;
    private final TelegramLinkService telegramLinkService;
    private final TelegramProperties telegramProperties;
    private final TelegramApiClient telegramApiClient;

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        Customer customer = createPendingCustomer(request);
        otpService.generateAndSend(customer.getEmail(), customer.getFullName(), OtpPurpose.REGISTER);
    }

    private Customer createPendingCustomer(RegisterRequest request) {
        String email = request.email().toLowerCase();
        if (superAdminProperties.matches(email)) {
            throw new DuplicateResourceException(
                    "This email is reserved for the administrator. Use a different email for a customer account.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("An account with this email already exists");
        }
        if (request.phoneNumber() != null && !request.phoneNumber().isBlank()
                && customerRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new DuplicateResourceException("An account with this phone number already exists");
        }

        Customer customer = new Customer();
        customer.setFullName(request.fullName());
        customer.setEmail(email);
        customer.setPassword(passwordEncoder.encode(request.password()));
        customer.setPhoneNumber(request.phoneNumber());
        customer.setGender(request.gender());
        customer.setStatus(UserStatus.PENDING_VERIFICATION);
        customer.setRegisterType(RegisterType.EMAIL);
        customerRepository.saveAndFlush(customer);
        authUserSyncService.sync(customer);
        return customer;
    }

    @Override
    @Transactional
    public void verifyRegistration(VerifyRegistrationRequest request) {
        String email = request.email().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No account found for this email"));

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new DuplicateResourceException("This account has already been verified");
        }

        otpService.verify(email, OtpPurpose.REGISTER, request.otp());
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        authUserSyncService.sync(user);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String email = request.email().toLowerCase();

        if (superAdminProperties.matches(email)) {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
            authUserSyncService.syncSuperAdmin();
            AuthTokenResponse tokens = issueTokens(SuperAdminUserDetails.ID, email, Role.SUPER_ADMIN);
            return LoginResponse.authenticated(tokens);
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No account found for this email"));

        otpService.generateAndSend(email, user.getFullName(), OtpPurpose.LOGIN, telegramDeepLinkFor(user));
        String ticket = tokenService.createLoginTicket(user.getId());
        return LoginResponse.otpChallenge(ticket);
    }

    @Override
    @Transactional
    public AuthTokenResponse loginViaTelegramWidget(TelegramWidgetAuthRequest request) {
        if (!TelegramWidgetAuthVerifier.isFresh(request)) {
            throw new InvalidCredentialsException("This Telegram login has expired — please try again");
        }
        if (!TelegramWidgetAuthVerifier.isValidSignature(request, telegramProperties.getBotToken())) {
            throw new InvalidCredentialsException("Invalid Telegram login signature");
        }

        User user = userRepository.findByTelegramChatId(String.valueOf(request.id()))
                .orElseGet(() -> registerCustomerViaTelegram(request));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidCredentialsException("This account can't log in right now");
        }
        return issueTokens(user.getId(), user.getEmail(), user.getRole());
    }

    private Customer registerCustomerViaTelegram(TelegramWidgetAuthRequest request) {
        if (request.username() == null || request.username().isBlank()) {
            throw new InvalidOperationException(
                    "Please set a username in Telegram (Settings → Username) before signing in");
        }

        Customer customer = new Customer();
        customer.setFullName(request.lastName() != null
                ? request.firstName() + " " + request.lastName()
                : request.firstName());
        customer.setEmail(TelegramAccountUtil.placeholderEmail());
        customer.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        customer.setAvatarUrl(request.photoUrl());
        customer.setStatus(UserStatus.ACTIVE);
        customer.setRegisterType(RegisterType.TELEGRAM);
        customer.setTelegramChatId(String.valueOf(request.id()));
        customer.setTelegramUsername(request.username());
        customerRepository.saveAndFlush(customer);
        authUserSyncService.sync(customer);

        telegramApiClient.sendHtmlMessageWithButtons(request.id(),
                "🎉 <b>Welcome, " + TelegramFormat.escape(TelegramFormat.titleCase(customer.getFullName())) + "!</b>\n\n"
                        + "You're now logged in — you'll get your order receipts, new event alerts, "
                        + "and reminders here from now on.");

        return customer;
    }

    @Override
    public LoginResponse loginViaPhone(PhoneLoginRequest request) {
        User user = Stream.concat(adminRepository.findAll().stream(), baristaRepository.findAll().stream())
                .filter(staff -> PhoneNumberUtil.matches(staff.getPhoneNumber(), request.phoneNumber()))
                .findFirst()
                .orElseThrow(() -> new InvalidCredentialsException(
                        "No staff account uses this phone number. Check the number or ask your admin."));

        if (user.getTelegramChatId() == null) {
            throw new InvalidOperationException(
                    "This number hasn't accepted its Telegram invite yet. Open the invite link first, "
                            + "or ask your admin to resend it.");
        }
        requireActive(user);

        otpService.generateAndSendViaTelegram(user.getEmail(), user.getFullName(), OtpPurpose.LOGIN,
                Long.parseLong(user.getTelegramChatId()));
        return LoginResponse.otpChallenge(tokenService.createLoginTicket(user.getId(), true));
    }

    @Override
    public AuthTokenResponse verifyLoginOtp(VerifyLoginOtpRequest request) {
        UUID userId = tokenService.peekLoginTicket(request.loginTicket());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account no longer exists"));

        otpService.verify(user.getEmail(), OtpPurpose.LOGIN, request.otp());
        tokenService.consumeLoginTicket(request.loginTicket());
        requireActive(user);
        return issueTokens(user.getId(), user.getEmail(), user.getRole());
    }

    @Override
    public void resendOtp(ResendOtpRequest request) {
        switch (request.purpose()) {
            case REGISTER -> {
                String email = requireEmail(request);
                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new ResourceNotFoundException("No account found for this email"));
                if (user.getStatus() == UserStatus.ACTIVE) {
                    throw new DuplicateResourceException("This account has already been verified");
                }
                if (user.getTelegramChatId() != null) {
                    otpService.resendViaTelegram(email, user.getFullName(), OtpPurpose.REGISTER,
                            Long.parseLong(user.getTelegramChatId()));
                } else {
                    otpService.resend(email, user.getFullName(), OtpPurpose.REGISTER);
                }
            }
            case LOGIN -> {
                if (request.loginTicket() == null || request.loginTicket().isBlank()) {
                    throw new InvalidCredentialsException("Login ticket is required to resend a login code");
                }
                UUID userId = tokenService.peekLoginTicket(request.loginTicket());
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Account no longer exists"));
                if (tokenService.isTelegramLoginTicket(request.loginTicket()) && user.getTelegramChatId() != null) {
                    otpService.resendViaTelegram(user.getEmail(), user.getFullName(), OtpPurpose.LOGIN,
                            Long.parseLong(user.getTelegramChatId()));
                } else {
                    otpService.resend(user.getEmail(), user.getFullName(), OtpPurpose.LOGIN, telegramDeepLinkFor(user));
                }
            }
            case RESET_PASSWORD -> {
                String email = requireEmail(request);
                userRepository.findByEmail(email)
                        .ifPresent(user -> otpService.resend(email, user.getFullName(), OtpPurpose.RESET_PASSWORD));
            }
        }
    }

    @Override
    public AuthTokenResponse refreshToken(RefreshTokenRequest request) {
        Claims claims;
        try {
            claims = jwtUtil.extractClaims(request.refreshToken());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidCredentialsException("Invalid or expired refresh token");
        }

        if (!jwtUtil.isRefreshToken(claims)) {
            throw new InvalidCredentialsException("Invalid or expired refresh token");
        }

        String email = claims.getSubject();
        UUID userId;
        Role role;
        if (superAdminProperties.matches(email)) {
            userId = SuperAdminUserDetails.ID;
            role = Role.SUPER_ADMIN;
        } else {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid or expired refresh token"));
            requireActive(user);
            userId = user.getId();
            role = user.getRole();
        }

        if (!tokenService.isRefreshTokenValid(userId, request.refreshToken())) {
            throw new InvalidCredentialsException("Refresh token has been revoked");
        }

        return issueTokens(userId, email, role);
    }

    @Override
    public void logout(String accessToken) {
        try {
            Claims claims = jwtUtil.extractClaims(accessToken);
            long remainingMillis = claims.getExpiration().getTime() - System.currentTimeMillis();
            tokenService.denylistAccessToken(claims.getId(), remainingMillis);

            String email = claims.getSubject();
            UUID userId = superAdminProperties.matches(email)
                    ? SuperAdminUserDetails.ID
                    : userRepository.findByEmail(email).map(User::getId).orElse(null);
            if (userId != null) {
                tokenService.revokeRefreshToken(userId);
            }
        } catch (JwtException | IllegalArgumentException ignored) {
        }
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.email().toLowerCase();
        userRepository.findByEmail(email)
                .ifPresent(user -> otpService.generateAndSend(email, user.getFullName(), OtpPurpose.RESET_PASSWORD));
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.email().toLowerCase();
        otpService.verify(email, OtpPurpose.RESET_PASSWORD, request.otp());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No account found for this email"));

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        authUserSyncService.sync(user);
        tokenService.revokeRefreshToken(user.getId());
    }

    private AuthTokenResponse issueTokens(UUID userId, String email, Role role) {
        String accessToken = jwtUtil.generateAccessToken(email, role.name());
        String refreshToken = jwtUtil.generateRefreshToken(email, role.name());
        tokenService.storeRefreshToken(userId, refreshToken);
        long expiresInMs = jwtUtil.getAccessExpirationMs();
        return new AuthTokenResponse(accessToken, refreshToken, "Bearer", expiresInMs, formatDuration(expiresInMs));
    }

    private String formatDuration(long millis) {
        Duration duration = Duration.ofMillis(millis);
        long days = duration.toDays();
        int hours = duration.toHoursPart();
        int minutes = duration.toMinutesPart();
        int seconds = duration.toSecondsPart();

        List<String> parts = new ArrayList<>();
        if (days > 0) {
            parts.add(days + (days == 1 ? " day" : " days"));
        }
        if (hours > 0) {
            parts.add(hours + (hours == 1 ? " hour" : " hours"));
        }
        if (minutes > 0) {
            parts.add(minutes + (minutes == 1 ? " minute" : " minutes"));
        }
        if (parts.isEmpty() && seconds > 0) {
            parts.add(seconds + (seconds == 1 ? " second" : " seconds"));
        }
        if (parts.isEmpty()) {
            parts.add("0 seconds");
        }
        return String.join(" ", parts);
    }

    private String telegramDeepLinkFor(User user) {
        if (user.getRole() != Role.CUSTOMER || user.getTelegramChatId() != null) {
            return null;
        }
        return telegramLinkService.generateLinkCode(user.getId()).deepLink();
    }

    private void requireActive(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidCredentialsException("This account can't log in right now");
        }
    }

    private String requireEmail(ResendOtpRequest request) {
        if (request.email() == null || request.email().isBlank()) {
            throw new InvalidCredentialsException("Email is required for this request");
        }
        return request.email().toLowerCase();
    }
}
