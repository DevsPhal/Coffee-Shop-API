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
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.enums.UserStatus;
import org.group1.coffeeshopapi.common.exception.DuplicateResourceException;
import org.group1.coffeeshopapi.common.exception.InvalidCredentialsException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.common.properties.SuperAdminProperties;
import org.group1.coffeeshopapi.common.security.SuperAdminUserDetails;
import org.group1.coffeeshopapi.common.util.JwtUtil;
import org.group1.coffeeshopapi.telegram.service.TelegramLinkService;
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

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final OtpService otpService;
    private final TokenService tokenService;
    private final JwtUtil jwtUtil;
    private final SuperAdminProperties superAdminProperties;
    private final AuthUserSyncService authUserSyncService;
    private final TelegramLinkService telegramLinkService;

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        String email = request.email().toLowerCase();
        if (superAdminProperties.matches(email)) {
            throw new DuplicateResourceException("This email is reserved");
        }
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("An account with this email already exists");
        }

        Customer customer = new Customer();
        customer.setFullName(request.fullName());
        customer.setEmail(email);
        customer.setPassword(passwordEncoder.encode(request.password()));
        customer.setPhoneNumber(request.phoneNumber());
        customer.setGender(request.gender());
        customer.setStatus(UserStatus.PENDING_VERIFICATION);
        customerRepository.saveAndFlush(customer);
        authUserSyncService.sync(customer);

        otpService.generateAndSend(email, customer.getFullName(), OtpPurpose.REGISTER);
    }

    @Override
    @Transactional
    public void verifyRegistration(VerifyRegistrationRequest request) {
        String email = request.email().toLowerCase();
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No account found for this email"));

        if (customer.getStatus() == UserStatus.ACTIVE) {
            throw new DuplicateResourceException("This account has already been verified");
        }

        otpService.verify(email, OtpPurpose.REGISTER, request.otp());
        customer.setStatus(UserStatus.ACTIVE);
        customerRepository.save(customer);
        authUserSyncService.sync(customer);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String email = request.email().toLowerCase();

        if (superAdminProperties.matches(email)) {
            // The super admin is a config-only account: no email inbox, no OTP friction, no DB row.
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
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
    public AuthTokenResponse verifyLoginOtp(VerifyLoginOtpRequest request) {
        UUID userId = tokenService.consumeLoginTicket(request.loginTicket());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account no longer exists"));

        otpService.verify(user.getEmail(), OtpPurpose.LOGIN, request.otp());
        return issueTokens(user.getId(), user.getEmail(), user.getRole());
    }

    @Override
    public void resendOtp(ResendOtpRequest request) {
        switch (request.purpose()) {
            case REGISTER -> {
                String email = requireEmail(request);
                Customer customer = customerRepository.findByEmail(email)
                        .orElseThrow(() -> new ResourceNotFoundException("No account found for this email"));
                if (customer.getStatus() == UserStatus.ACTIVE) {
                    throw new DuplicateResourceException("This account has already been verified");
                }
                otpService.resend(email, customer.getFullName(), OtpPurpose.REGISTER);
            }
            case LOGIN -> {
                if (request.loginTicket() == null || request.loginTicket().isBlank()) {
                    throw new InvalidCredentialsException("Login ticket is required to resend a login code");
                }
                UUID userId = tokenService.peekLoginTicket(request.loginTicket());
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Account no longer exists"));
                otpService.resend(user.getEmail(), user.getFullName(), OtpPurpose.LOGIN, telegramDeepLinkFor(user));
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
            // Already invalid/expired token — logout is idempotent either way.
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

    // e.g. 86400000 -> "1 day", 90000 -> "1 minute 30 seconds". Seconds are dropped once the
    // duration reaches a day/hour/minute, since they're not meaningful at that granularity.
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

    /**
     * A login OTP email offers a "Connect Telegram" button only for customers who haven't
     * linked a chat yet — staff/super-admin have no Telegram concept, and an already-linked
     * customer doesn't need another code.
     */
    private String telegramDeepLinkFor(User user) {
        if (user.getRole() != Role.CUSTOMER || user.getTelegramChatId() != null) {
            return null;
        }
        return telegramLinkService.generateLinkCode(user.getId()).deepLink();
    }

    private String requireEmail(ResendOtpRequest request) {
        if (request.email() == null || request.email().isBlank()) {
            throw new InvalidCredentialsException("Email is required for this request");
        }
        return request.email().toLowerCase();
    }
}