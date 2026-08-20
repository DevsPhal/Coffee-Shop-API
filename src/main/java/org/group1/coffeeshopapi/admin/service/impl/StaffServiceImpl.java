package org.group1.coffeeshopapi.admin.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.admin.dto.request.CreateStaffRequest;
import org.group1.coffeeshopapi.admin.dto.request.UpdateStaffRequest;
import org.group1.coffeeshopapi.admin.entity.Admin;
import org.group1.coffeeshopapi.admin.repository.AdminRepository;
import org.group1.coffeeshopapi.admin.service.StaffService;
import org.group1.coffeeshopapi.auth.service.TokenService;
import org.group1.coffeeshopapi.barista.entity.Barista;
import org.group1.coffeeshopapi.barista.repository.BaristaRepository;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.common.exception.DuplicateResourceException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.common.properties.SuperAdminProperties;
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

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StaffServiceImpl implements StaffService {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final BaristaRepository baristaRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final SuperAdminProperties superAdminProperties;
    private final TokenService tokenService;
    private final AuthUserSyncService authUserSyncService;

    @Override
    @Transactional
    public UserResponse create(CreateStaffRequest request, Role role) {
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
        // Created directly by a higher-privileged role, so it's trusted — active immediately, no OTP verification.
        staff.setStatus(Status.ACTIVE);

        if (staff instanceof Admin admin) {
            adminRepository.save(admin);
        } else {
            baristaRepository.save((Barista) staff);
        }
        authUserSyncService.sync(staff);
        return userMapper.toResponse(staff);
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
            if (request.status() == Status.INACTIVE) {
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