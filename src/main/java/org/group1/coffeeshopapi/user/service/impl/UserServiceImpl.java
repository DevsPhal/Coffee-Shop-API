package org.group1.coffeeshopapi.user.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group1.coffeeshopapi.user.entity.User;
import org.group1.coffeeshopapi.user.mapper.UserMapper;
import org.group1.coffeeshopapi.user.repository.UserRepository;
import org.group1.coffeeshopapi.user.service.UserService;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.common.responses.PaginatedResponse;
import org.group1.coffeeshopapi.common.security.CustomUserDetails;
import org.group1.coffeeshopapi.common.utils.PageUtil;
import org.group1.coffeeshopapi.user.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        return userMapper.toResponse(user);
    }

    private UUID getCurrentUserId() {
        CustomUserDetails principal = (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        return principal.getId();
    }

    @Override
    public PaginatedResponse<UserResponse> getAllUsers(Pageable pageable) {
        Page<UserResponse> page = userRepository.findAll(pageable).map(userMapper::toResponse);
        return PageUtil.toPaginatedResponse(page);
    }

    @Override
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        return userMapper.toResponse(user);
    }

    @Override
    public void delete(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));

        if (user.getId().equals(getCurrentUserId())) {
            throw new InvalidOperationException("Admins cannot delete their own account.");
        }

        userRepository.delete(user);
    }

    @Override
    public UserResponse updateRole(UUID id, Role role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));

        if (user.getId().equals(getCurrentUserId())) {
            throw new InvalidOperationException("Admins cannot change their own role.");
        }

        user.setRole(role);
        userRepository.save(user);
        return userMapper.toResponse(user);
    }
}
