package org.group1.coffeeshopapi.user.service;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.enums.UserStatus;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.security.SuperAdminUserDetails;
import org.group1.coffeeshopapi.common.storage.FileStorageService;
import org.group1.coffeeshopapi.user.dto.request.UpdateProfileRequest;
import org.group1.coffeeshopapi.user.dto.response.SuperAdminResponse;
import org.group1.coffeeshopapi.user.entity.SuperAdminProfile;
import org.group1.coffeeshopapi.user.repository.SuperAdminProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Name, phone, gender and photo for the config-driven super admin, which has no users row. The same
 * rules as UserProfileServiceImpl apply; email and password stay in the deployment config.
 */
@Service
@RequiredArgsConstructor
public class SuperAdminProfileService {
    private static final String IMAGE_FOLDER = "avatars";

    private final SuperAdminProfileRepository superAdminProfileRepository;
    private final FileStorageService fileStorageService;
    private final AuthUserSyncService authUserSyncService;

    public SuperAdminResponse getProfile(String email) {
        return toResponse(load(), email);
    }

    @Transactional
    public SuperAdminResponse updateProfile(String email, UpdateProfileRequest request) {
        SuperAdminProfile profile = load();
        if (request.fullName() != null) {
            String fullName = request.fullName().trim();
            if (fullName.isEmpty()) {
                throw new InvalidOperationException("Full name cannot be blank");
            }
            profile.setFullName(fullName);
        }
        if (request.phoneNumber() != null) {
            String phoneNumber = request.phoneNumber().trim();
            profile.setPhoneNumber(phoneNumber.isEmpty() ? null : phoneNumber);
        }
        if (request.gender() != null) {
            profile.setGender(request.gender());
        }
        profile = superAdminProfileRepository.save(profile);
        // Audit logs and "created by" columns read the name from auth_users.
        authUserSyncService.syncSuperAdmin();
        return toResponse(profile, email);
    }

    @Transactional
    public SuperAdminResponse uploadAvatar(String email, MultipartFile file) {
        SuperAdminProfile profile = load();
        String previousAvatarUrl = profile.getAvatarUrl();
        profile.setAvatarUrl(fileStorageService.uploadImage(file, IMAGE_FOLDER));
        profile = superAdminProfileRepository.save(profile);
        if (previousAvatarUrl != null) {
            fileStorageService.delete(previousAvatarUrl);
        }
        return toResponse(profile, email);
    }

    @Transactional
    public SuperAdminResponse removeAvatar(String email) {
        SuperAdminProfile profile = load();
        if (profile.getAvatarUrl() != null) {
            fileStorageService.delete(profile.getAvatarUrl());
            profile.setAvatarUrl(null);
            profile = superAdminProfileRepository.save(profile);
        }
        return toResponse(profile, email);
    }

    // The row is only written once the super admin saves something; until then the built-in defaults apply.
    private SuperAdminProfile load() {
        return superAdminProfileRepository.findProfile().orElseGet(() -> {
            SuperAdminProfile profile = new SuperAdminProfile();
            profile.setId(SuperAdminUserDetails.ID);
            profile.setFullName(SuperAdminUserDetails.DISPLAY_NAME);
            return profile;
        });
    }

    private SuperAdminResponse toResponse(SuperAdminProfile profile, String email) {
        return SuperAdminResponse.builder()
                .id(SuperAdminUserDetails.ID)
                .fullName(profile.getFullName())
                .email(email)
                .phoneNumber(profile.getPhoneNumber())
                .avatarUrl(profile.getAvatarUrl())
                .gender(profile.getGender())
                .role(Role.SUPER_ADMIN)
                .status(UserStatus.ACTIVE)
                .build();
    }
}
