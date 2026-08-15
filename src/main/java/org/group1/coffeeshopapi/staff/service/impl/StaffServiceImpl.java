package org.group1.coffeeshopapi.staff.service.impl;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.group1.coffeeshopapi.admin.entity.User;
import org.group1.coffeeshopapi.admin.repository.UserRepository;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.exception.InvalidRequestException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.common.responses.PageResponse;
import org.group1.coffeeshopapi.common.utils.PageUtil;
import org.group1.coffeeshopapi.staff.dto.request.StaffRequest;
import org.group1.coffeeshopapi.staff.dto.response.StaffResponse;
import org.group1.coffeeshopapi.staff.service.StaffService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class StaffServiceImpl implements StaffService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StaffResponse> getAllStaff(Pageable pageable) {
        Page<StaffResponse> page = userRepository.findByRoleNot(Role.CUSTOMER, pageable).map(this::toResponse);
        return PageUtil.toPageResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public StaffResponse getStaffById(UUID id) {
        User user = userRepository.findById(id)
                .filter(candidate -> candidate.getRole() != Role.CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + id));
        return toResponse(user);
    }

    @Override
    public StaffResponse createStaff(StaffRequest request) {
        User user = new User();
        user.setFullName(request.getFullName());
        user.setGivenName(request.getFullName());
        user.setFamilyName("");
        user.setUsername(request.getUsername() != null && !request.getUsername().isBlank()
                ? request.getUsername()
                : request.getEmail());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhoneNumber(request.getPhoneNumber());
        user.setRole(parseRole(request.getRole()));
        user.setEnabled(!"INACTIVE".equalsIgnoreCase(request.getStatus()));

        return toResponse(userRepository.save(user));
    }

    @Override
    public StaffResponse updateStaff(UUID id, StaffRequest request) {
        User user = userRepository.findById(id)
                .filter(candidate -> candidate.getRole() != Role.CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + id));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
            user.setGivenName(request.getFullName());
        }
        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getRole() != null) {
            user.setRole(parseRole(request.getRole()));
        }
        if (request.getStatus() != null) {
            user.setEnabled(!"INACTIVE".equalsIgnoreCase(request.getStatus()));
        }

        return toResponse(userRepository.save(user));
    }

    @Override
    public void deleteStaff(UUID id) {
        User user = userRepository.findById(id)
                .filter(candidate -> candidate.getRole() != Role.CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + id));
        userRepository.delete(user);
    }

    private StaffResponse toResponse(User user) {
        return StaffResponse.builder()
                .id(user.getId())
                .name(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhoneNumber())
                .role(user.getRole().name())
                .status(user.isEnabled() ? "ACTIVE" : "INACTIVE")
                .joinDate(user.getCreatedAt() == null ? null : user.getCreatedAt().format(DATE_FORMAT))
                .build();
    }

    private Role parseRole(String value) {
        if (value == null || value.isBlank()) {
            return Role.BARISTA;
        }
        Role role;
        try {
            role = Role.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Invalid staff role: " + value + ". Allowed: ADMIN, BARISTA");
        }
        if (role != Role.ADMIN && role != Role.BARISTA) {
            throw new InvalidRequestException("Invalid staff role: " + value + ". Allowed: ADMIN, BARISTA");
        }
        return role;
    }
}
