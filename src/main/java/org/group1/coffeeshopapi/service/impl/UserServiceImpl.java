package org.group1.coffeeshopapi.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.dto.common.ApiResponse;
import org.group1.coffeeshopapi.dto.response.UserResponse;
import org.group1.coffeeshopapi.entity.enums.Role;
import org.group1.coffeeshopapi.entity.User;
import org.group1.coffeeshopapi.exception.UnauthorizedException;
import org.group1.coffeeshopapi.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.repository.UserRepository;
import org.group1.coffeeshopapi.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    @Override
    public UserResponse getCurrentUser() {
        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())){
            throw new UnauthorizedException("Authentication required.");
        }

        String email = auth.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return mapToResponse(user);
    }

    @Override
    public UserResponse getUserById(UUID uId) {
        UserResponse currentUser = getCurrentUser();
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        boolean isSelf = currentUser.getId().equals(uId);
        if (!isAdmin && !isSelf){
            throw new AccessDeniedException("You don't have permission to view this user");
        }

        User user = userRepository.findById(uId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return mapToResponse(user);
    }

    @Override
    public List<UserResponse> getAllUsers() {
        UserResponse currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.ADMIN){
            throw new AccessDeniedException("Admin access required");
        }

        return userRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public ApiResponse<Void> deleteUserById(UUID uid) {
        UserResponse currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.ADMIN){
            throw new AccessDeniedException("Admin access required");
        }

        User user = userRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        userRepository.delete(user);

        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("User deleted successfully")
                .timeStamp(LocalDateTime.now())
                .build();
    }
}
