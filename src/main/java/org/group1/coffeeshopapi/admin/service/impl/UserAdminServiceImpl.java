package org.group1.coffeeshopapi.admin.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.admin.repository.AdminRepository;
import org.group1.coffeeshopapi.admin.service.UserAdminService;
import org.group1.coffeeshopapi.auth.service.TokenService;
import org.group1.coffeeshopapi.barista.repository.BaristaRepository;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.enums.UserStatus;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.user.dto.request.UpdateUserStatusRequest;
import org.group1.coffeeshopapi.user.dto.response.UserResponse;
import org.group1.coffeeshopapi.user.entity.User;
import org.group1.coffeeshopapi.user.mapper.UserMapper;
import org.group1.coffeeshopapi.user.repository.CustomerRepository;
import org.group1.coffeeshopapi.user.repository.UserRepository;
import org.group1.coffeeshopapi.user.service.AuthUserSyncService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAdminServiceImpl implements UserAdminService {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final BaristaRepository baristaRepository;
    private final CustomerRepository customerRepository;
    private final UserMapper userMapper;
    private final TokenService tokenService;
    private final AuthUserSyncService authUserSyncService;

    @Override
    public Page<UserResponse> list(Role roleFilter, Pageable pageable) {
        Page<? extends User> users = switch (roleFilter) {
            case null -> userRepository.findAll(pageable);
            case ADMIN -> adminRepository.findAll(pageable);
            case BARISTA -> baristaRepository.findAll(pageable);
            case CUSTOMER -> customerRepository.findAll(pageable);
            case SUPER_ADMIN -> throw new IllegalArgumentException("Super admin is not a stored account");
        };
        return users.map(userMapper::toResponse);
    }

    @Override
    public UserResponse getById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateStatus(UUID id, UpdateUserStatusRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setStatus(request.status());
        userRepository.save(user);
        authUserSyncService.sync(user);
        if (request.status() != UserStatus.ACTIVE) {
            // Block new access tokens immediately; JwtAuthFilter's isEnabled() re-check handles
            // any already-issued access token still inside its lifetime.
            tokenService.revokeRefreshToken(user.getId());
        }

        return userMapper.toResponse(user);
    }
}
