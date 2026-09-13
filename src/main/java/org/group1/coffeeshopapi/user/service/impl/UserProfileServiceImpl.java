package org.group1.coffeeshopapi.user.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.auth.service.TokenService;
import org.group1.coffeeshopapi.common.exception.DuplicateResourceException;
import org.group1.coffeeshopapi.common.exception.InvalidCredentialsException;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.common.storage.FileStorageService;
import org.group1.coffeeshopapi.user.dto.request.ChangePasswordRequest;
import org.group1.coffeeshopapi.user.dto.request.UpdateProfileRequest;
import org.group1.coffeeshopapi.user.dto.response.UserResponse;
import org.group1.coffeeshopapi.user.entity.User;
import org.group1.coffeeshopapi.user.mapper.UserMapper;
import org.group1.coffeeshopapi.user.repository.UserRepository;
import org.group1.coffeeshopapi.user.service.AuthUserSyncService;
import org.group1.coffeeshopapi.user.service.UserProfileService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private static final String IMAGE_FOLDER = "avatars";

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final FileStorageService fileStorageService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AuthUserSyncService authUserSyncService;

    @Override
    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findById(userId);

        if (request.fullName() != null) {
            String fullName = request.fullName().trim();
            if (fullName.isEmpty()) {
                throw new InvalidOperationException("Full name cannot be blank");
            }
            user.setFullName(fullName);
        }
        // Blank phone number clears the field; null leaves it untouched. Gender has no such
        // "clear" form — the enum has no empty member, so null can only mean "unchanged".
        if (request.phoneNumber() != null) {
            String phoneNumber = request.phoneNumber().trim();
            user.setPhoneNumber(phoneNumber.isEmpty() ? null : phoneNumber);
        }
        if (request.gender() != null) {
            user.setGender(request.gender());
        }

        // Phone numbers carry a unique constraint per role table (uk_admins_phone_number and
        // friends), so flush here to turn a collision into a 409 with a usable message instead
        // of letting it surface as a 500 at commit time. The constraint is per-table, which is
        // why this defers to the database rather than pre-checking across all roles.
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateResourceException("That phone number is already used by another account");
        }
        authUserSyncService.sync(user);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = findById(userId);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new InvalidOperationException("The new password must be different from the current one");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        authUserSyncService.sync(user);

        // Every other session signed in with the old password is now stale — drop the refresh
        // token so they cannot silently renew. The caller's own access token stays valid until
        // it expires, so the change does not sign the user out of the tab they made it in.
        tokenService.revokeRefreshToken(user.getId());
    }

    @Override
    @Transactional
    public UserResponse uploadAvatar(UUID userId, MultipartFile file) {
        User user = findById(userId);
        String previousAvatarUrl = user.getAvatarUrl();

        user.setAvatarUrl(fileStorageService.uploadImage(file, IMAGE_FOLDER));
        user = userRepository.save(user);

        if (previousAvatarUrl != null) {
            fileStorageService.delete(previousAvatarUrl);
        }

        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse removeAvatar(UUID userId) {
        User user = findById(userId);
        if (user.getAvatarUrl() != null) {
            fileStorageService.delete(user.getAvatarUrl());
            user.setAvatarUrl(null);
            user = userRepository.save(user);
        }
        return userMapper.toResponse(user);
    }

    private User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
