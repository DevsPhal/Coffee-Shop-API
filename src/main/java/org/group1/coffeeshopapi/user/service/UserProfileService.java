package org.group1.coffeeshopapi.user.service;

import org.group1.coffeeshopapi.user.dto.response.UserResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/** Self-service profile updates for the currently authenticated account. */
public interface UserProfileService {
    UserResponse uploadAvatar(UUID userId, MultipartFile file);
    UserResponse removeAvatar(UUID userId);
}