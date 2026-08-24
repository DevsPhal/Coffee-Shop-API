package org.group1.coffeeshopapi.user.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.common.storage.FileStorageService;
import org.group1.coffeeshopapi.user.dto.response.UserResponse;
import org.group1.coffeeshopapi.user.entity.User;
import org.group1.coffeeshopapi.user.mapper.UserMapper;
import org.group1.coffeeshopapi.user.repository.UserRepository;
import org.group1.coffeeshopapi.user.service.UserProfileService;
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