package org.group1.coffeeshopapi.user.service;

import org.group1.coffeeshopapi.user.dto.request.ChangePasswordRequest;
import org.group1.coffeeshopapi.user.dto.request.CompleteProfileRequest;
import org.group1.coffeeshopapi.user.dto.request.UpdateProfileRequest;
import org.group1.coffeeshopapi.user.dto.response.UserResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/** Self-service profile updates for the currently authenticated account. */
public interface UserProfileService {
    UserResponse updateProfile(UUID userId, UpdateProfileRequest request);
    UserResponse completeProfile(UUID userId, CompleteProfileRequest request);
    void changePassword(UUID userId, ChangePasswordRequest request);
    UserResponse uploadAvatar(UUID userId, MultipartFile file);
    UserResponse removeAvatar(UUID userId);
}
