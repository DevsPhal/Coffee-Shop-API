package org.group1.coffeeshopapi.admin.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.admin.dto.request.CreateStaffRequest;
import org.group1.coffeeshopapi.admin.dto.request.InviteStaffRequest;
import org.group1.coffeeshopapi.admin.dto.request.UpdateStaffRequest;
import org.group1.coffeeshopapi.admin.entity.Admin;
import org.group1.coffeeshopapi.admin.repository.AdminRepository;
import org.group1.coffeeshopapi.admin.service.StaffService;
import org.group1.coffeeshopapi.auth.service.TokenService;
import org.group1.coffeeshopapi.barista.entity.Barista;
import org.group1.coffeeshopapi.barista.repository.BaristaRepository;
import org.group1.coffeeshopapi.common.enums.RegisterType;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.enums.UserStatus;
import org.group1.coffeeshopapi.common.exception.DuplicateResourceException;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.common.properties.SuperAdminProperties;
import org.group1.coffeeshopapi.common.storage.FileStorageService;
import org.group1.coffeeshopapi.telegram.dto.TelegramLinkCodeResponse;
import org.group1.coffeeshopapi.telegram.service.TelegramLinkService;
import org.group1.coffeeshopapi.telegram.util.TelegramAccountUtil;
import org.group1.coffeeshopapi.user.dto.response.UserResponse;
import org.group1.coffeeshopapi.user.entity.User;
import org.group1.coffeeshopapi.user.mapper.UserMapper;
import org.group1.coffeeshopapi.user.repository.UserRepository;
import org.group1.coffeeshopapi.user.service.AuthUserSyncService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StaffServiceImpl implements StaffService {

    private static final String AVATAR_FOLDER = "avatars";

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final BaristaRepository baristaRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final SuperAdminProperties superAdminProperties;
    private final TokenService tokenService;
    private final AuthUserSyncService authUserSyncService;
    private final TelegramLinkService telegramLinkService;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public UserResponse create(CreateStaffRequest request, Role role, UUID createdBy) {
        // Created directly by a higher-privileged role, so it's trusted — active immediately, no
        // OTP verification (unlike createViaTelegram, which isn't trusted until the invitee
        // proves they own that Telegram chat).
        User staff = buildStaff(request, role, createdBy, UserStatus.ACTIVE, RegisterType.EMAIL);
        authUserSyncService.sync(staff);
        return userMapper.toResponse(staff);
    }

    @Override
    @Transactional
    public TelegramLinkCodeResponse createViaTelegram(InviteStaffRequest request, Role role, UUID createdBy) {
        User staff = buildInvitedStaff(request, role, createdBy);
        authUserSyncService.sync(staff);
        // Can't verify them yet — Telegram only allows messaging a chat the invitee has opened.
        // Returning a link code instead; opening it is what actually prompts them to share their
        // contact for phone-number verification (see TelegramLinkServiceImpl#resolveLinkCode).
        return telegramLinkService.generateLinkCode(staff.getId());
    }

    @Override
    public TelegramLinkCodeResponse resendTelegramInvite(UUID id, Role role) {
        User staff = findByIdAndRole(id, role);
        if (staff.getRegisterType() != RegisterType.TELEGRAM) {
            throw new InvalidOperationException("This account wasn't created via a Telegram invite");
        }
        if (staff.getStatus() != UserStatus.PENDING_VERIFICATION) {
            throw new DuplicateResourceException("This account has already been verified");
        }
        return telegramLinkService.generateLinkCode(staff.getId());
    }

    private User buildStaff(CreateStaffRequest request, Role role, UUID createdBy, UserStatus status,
                             RegisterType registerType) {
        String email = request.email().toLowerCase();
        if (superAdminProperties.matches(email)) {
            throw new DuplicateResourceException("This email is reserved");
        }
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("An account with this email already exists");
        }

        User staff = switch (role) {
            case ADMIN -> new Admin();
            case BARISTA -> new Barista();
            default -> throw new IllegalArgumentException("Unsupported staff role: " + role);
        };
        staff.setFullName(request.fullName());
        staff.setEmail(email);
        staff.setPassword(passwordEncoder.encode(request.password()));
        staff.setPhoneNumber(request.phoneNumber());
        staff.setGender(request.gender());
        staff.setStatus(status);
        staff.setRegisterType(registerType);

        if (staff instanceof Admin admin) {
            admin.setCreatedBy(createdBy);
            adminRepository.save(admin);
        } else {
            Barista barista = (Barista) staff;
            barista.setCreatedByAdmin(adminRepository.referenceOrNull(createdBy));
            baristaRepository.save(barista);
        }
        return staff;
    }

    private User buildInvitedStaff(InviteStaffRequest request, Role role, UUID createdBy) {
        User staff = switch (role) {
            case ADMIN -> new Admin();
            case BARISTA -> new Barista();
            default -> throw new IllegalArgumentException("Unsupported staff role: " + role);
        };
        staff.setFullName(request.fullName());
        // No email/password collected for an invite — login is Telegram-widget-only afterward
        // (AuthServiceImpl#loginViaTelegramWidget). See TelegramAccountUtil for why both still get
        // an unguessable placeholder. UserMapper#toResponse hides the placeholder email from API
        // responses.
        staff.setEmail(TelegramAccountUtil.placeholderEmail());
        staff.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        staff.setPhoneNumber(request.phoneNumber());
        staff.setGender(request.gender());
        staff.setStatus(UserStatus.PENDING_VERIFICATION);
        staff.setRegisterType(RegisterType.TELEGRAM);

        if (staff instanceof Admin admin) {
            admin.setCreatedBy(createdBy);
            adminRepository.save(admin);
        } else {
            Barista barista = (Barista) staff;
            barista.setCreatedByAdmin(adminRepository.referenceOrNull(createdBy));
            baristaRepository.save(barista);
        }
        return staff;
    }

    @Override
    public UserResponse getById(UUID id, Role role) {
        return userMapper.toResponse(findByIdAndRole(id, role));
    }

    @Override
    public Page<UserResponse> list(Role role, Pageable pageable) {
        return switch (role) {
            case ADMIN -> adminRepository.findAll(pageable).map(userMapper::toResponse);
            case BARISTA -> baristaRepository.findAll(pageable).map(userMapper::toResponse);
            default -> throw new IllegalArgumentException("Unsupported staff role: " + role);
        };
    }

    @Override
    @Transactional
    public UserResponse update(UUID id, UpdateStaffRequest request, Role role) {
        User staff = findByIdAndRole(id, role);

        if (request.fullName() != null) {
            staff.setFullName(request.fullName());
        }
        if (request.phoneNumber() != null) {
            staff.setPhoneNumber(request.phoneNumber());
        }
        if (request.gender() != null) {
            staff.setGender(request.gender());
        }
        if (request.status() != null) {
            staff.setStatus(request.status());
            if (request.status() != UserStatus.ACTIVE) {
                // Block new access tokens immediately; JwtAuthFilter's isEnabled() re-check
                // handles any already-issued access token still inside its lifetime.
                tokenService.revokeRefreshToken(staff.getId());
            }
        }

        userRepository.save(staff);
        authUserSyncService.sync(staff);
        return userMapper.toResponse(staff);
    }

    @Override
    @Transactional
    public UserResponse uploadAvatar(UUID id, MultipartFile file, Role role) {
        User staff = findByIdAndRole(id, role);
        String previousAvatarUrl = staff.getAvatarUrl();

        staff.setAvatarUrl(fileStorageService.uploadImage(file, AVATAR_FOLDER));
        userRepository.save(staff);

        if (previousAvatarUrl != null) {
            fileStorageService.delete(previousAvatarUrl);
        }
        return userMapper.toResponse(staff);
    }

    @Override
    @Transactional
    public void delete(UUID id, Role role) {
        User staff = findByIdAndRole(id, role);
        tokenService.revokeRefreshToken(staff.getId());
        userRepository.delete(staff);
        authUserSyncService.remove(staff.getId());
    }

    private User findByIdAndRole(UUID id, Role role) {
        return switch (role) {
            case ADMIN -> adminRepository.findById(id).map(a -> (User) a)
                    .orElseThrow(() -> new ResourceNotFoundException(role.name() + " not found"));
            case BARISTA -> baristaRepository.findById(id).map(b -> (User) b)
                    .orElseThrow(() -> new ResourceNotFoundException(role.name() + " not found"));
            default -> throw new IllegalArgumentException("Unsupported staff role: " + role);
        };
    }
}