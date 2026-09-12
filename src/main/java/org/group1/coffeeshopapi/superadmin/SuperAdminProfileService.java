package org.group1.coffeeshopapi.superadmin;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.enums.UserStatus;
import org.group1.coffeeshopapi.common.security.SuperAdminUserDetails;
import org.group1.coffeeshopapi.common.storage.FileStorageService;
import org.group1.coffeeshopapi.user.dto.request.UpdateProfileRequest;
import org.group1.coffeeshopapi.user.dto.response.SuperAdminResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Maintains the super admin's display profile. The credentials it signs in with are not stored
 * here and never will be — see {@link SuperAdminProfile} for why.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SuperAdminProfileService {

    private static final String IMAGE_FOLDER = "avatars";
    private static final String DEFAULT_NAME = "Super Admin";

    private final SuperAdminProfileRepository repository;
    private final FileStorageService fileStorageService;

    /** The profile as it should be shown, with the configured email as the one fixed field. */
    public SuperAdminResponse describe(String email) {
        return repository.findById(SuperAdminUserDetails.ID)
                .map(profile -> toResponse(profile, email))
                .orElseGet(() -> SuperAdminResponse.builder()
                        .id(SuperAdminUserDetails.ID)
                        .fullName(DEFAULT_NAME)
                        .email(email)
                        .role(Role.SUPER_ADMIN)
                        .status(UserStatus.ACTIVE)
                        .build());
    }

    @Transactional
    public SuperAdminResponse updateProfile(UpdateProfileRequest request, String email) {
        SuperAdminProfile profile = loadOrCreate();

        if (request.fullName() != null) {
            String fullName = request.fullName().trim();
            profile.setFullName(fullName.isEmpty() ? null : fullName);
        }
        if (request.phoneNumber() != null) {
            // An empty string clears the number; UpdateProfileRequest allows it for exactly this.
            String phoneNumber = request.phoneNumber().trim();
            profile.setPhoneNumber(phoneNumber.isEmpty() ? null : phoneNumber);
        }
        if (request.gender() != null) {
            profile.setGender(request.gender());
        }

        return toResponse(repository.save(profile), email);
    }

    @Transactional
    public SuperAdminResponse uploadAvatar(MultipartFile file, String email) {
        SuperAdminProfile profile = loadOrCreate();
        String previousAvatarUrl = profile.getAvatarUrl();

        profile.setAvatarUrl(fileStorageService.uploadImage(file, IMAGE_FOLDER));
        profile = repository.save(profile);

        if (previousAvatarUrl != null) {
            fileStorageService.delete(previousAvatarUrl);
        }
        return toResponse(profile, email);
    }

    @Transactional
    public SuperAdminResponse removeAvatar(String email) {
        SuperAdminProfile profile = loadOrCreate();
        if (profile.getAvatarUrl() != null) {
            fileStorageService.delete(profile.getAvatarUrl());
            profile.setAvatarUrl(null);
            profile = repository.save(profile);
        }
        return toResponse(profile, email);
    }

    private SuperAdminProfile loadOrCreate() {
        return repository.findById(SuperAdminUserDetails.ID).orElseGet(() -> {
            SuperAdminProfile created = new SuperAdminProfile();
            created.setId(SuperAdminUserDetails.ID);
            return created;
        });
    }

    private SuperAdminResponse toResponse(SuperAdminProfile profile, String email) {
        return SuperAdminResponse.builder()
                .id(SuperAdminUserDetails.ID)
                .fullName(profile.getFullName() != null ? profile.getFullName() : DEFAULT_NAME)
                .email(email)
                .phoneNumber(profile.getPhoneNumber())
                .gender(profile.getGender())
                .avatarUrl(profile.getAvatarUrl())
                .role(Role.SUPER_ADMIN)
                .status(UserStatus.ACTIVE)
                .build();
    }
}
