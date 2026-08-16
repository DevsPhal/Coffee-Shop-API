package org.group1.coffeeshopapi.staff.service.impl;

import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.group1.coffeeshopapi.user.entity.User;
import org.group1.coffeeshopapi.user.repository.UserRepository;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.exception.InvalidRequestException;
import org.group1.coffeeshopapi.common.responses.PaginatedResponse;
import org.group1.coffeeshopapi.common.utils.PageUtil;
import org.group1.coffeeshopapi.staff.dto.request.StaffRequest;
import org.group1.coffeeshopapi.staff.dto.response.StaffResponse;
import org.group1.coffeeshopapi.staff.mapper.StaffMapper;
import org.group1.coffeeshopapi.staff.service.StaffService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
@Transactional
@Slf4j
public class StaffServiceImpl implements StaffService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StaffMapper staffMapper;

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<StaffResponse> getAllStaff(Pageable pageable) {
        Page<StaffResponse> page = userRepository.findByRoleNot(Role.CUSTOMER, pageable).map(staffMapper::toResponse);
        return PageUtil.toPaginatedResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public StaffResponse getStaffById(UUID id) {
        User user = userRepository.findById(id)
                .filter(candidate -> candidate.getRole() != Role.CUSTOMER)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found: " + id));
        return staffMapper.toResponse(user);
    }

    @Override
    public StaffResponse createStaff(StaffRequest request) {
        validateUniqueStaff(request, null);
        log.info("Staff before saving: {}", request);
        User user = staffMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(parseRole(request.getRole()));
        user.setEnabled(!"INACTIVE".equalsIgnoreCase(request.getStatus()));

        User saved = userRepository.save(user);
        log.info("Staff after saving: {}", saved.getId());
        return staffMapper.toResponse(saved);
    }

    @Override
    public StaffResponse updateStaff(UUID id, StaffRequest request) {
        User user = userRepository.findById(id)
                .filter(candidate -> candidate.getRole() != Role.CUSTOMER)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found: " + id));

        validateUniqueStaff(request, id);
        staffMapper.updateEntity(request, user);
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getRole() != null) {
            user.setRole(parseRole(request.getRole()));
        }
        if (request.getStatus() != null) {
            user.setEnabled(!"INACTIVE".equalsIgnoreCase(request.getStatus()));
        }

        return staffMapper.toResponse(userRepository.save(user));
    }

    @Override
    public void deleteStaff(UUID id) {
        User user = userRepository.findById(id)
                .filter(candidate -> candidate.getRole() != Role.CUSTOMER)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found: " + id));
        userRepository.delete(user);
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
        if (role == Role.CUSTOMER) {
            throw new InvalidRequestException("Staff cannot be assigned the CUSTOMER role");
        }
        return role;
    }

    private void validateUniqueStaff(StaffRequest request, UUID currentId) {
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()
                && (currentId == null
                ? userRepository.existsByPhoneNumber(request.getPhoneNumber())
                : userRepository.existsByPhoneNumberAndIdNot(request.getPhoneNumber(), currentId))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number already exists");
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && (currentId == null
                ? userRepository.existsByEmail(request.getEmail())
                : userRepository.existsByEmailAndIdNot(request.getEmail(), currentId))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        if (request.getUsername() != null && !request.getUsername().isBlank()
                && (currentId == null
                ? userRepository.existsByUsername(request.getUsername())
                : userRepository.existsByUsernameAndIdNot(request.getUsername(), currentId))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
    }
}
