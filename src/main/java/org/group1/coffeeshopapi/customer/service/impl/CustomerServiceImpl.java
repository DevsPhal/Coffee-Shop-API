package org.group1.coffeeshopapi.customer.service.impl;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.admin.entity.User;
import org.group1.coffeeshopapi.admin.repository.UserRepository;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.common.responses.PageResponse;
import org.group1.coffeeshopapi.common.utils.PageUtil;
import org.group1.coffeeshopapi.customer.dto.request.CustomerRequest;
import org.group1.coffeeshopapi.customer.dto.response.CustomerResponse;
import org.group1.coffeeshopapi.customer.mapper.CustomerMapper;
import org.group1.coffeeshopapi.customer.service.CustomerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional

public class CustomerServiceImpl implements CustomerService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomerMapper customerMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> getAllCustomers(Pageable pageable) {
        Page<CustomerResponse> page = userRepository.findByRole(Role.CUSTOMER, pageable).map(this::toResponse);
        return PageUtil.toPageResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(UUID id) {
        User user = userRepository.findById(id)
                .filter(candidate -> candidate.getRole() == Role.CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
        return toResponse(user);
    }

    @Override
    public CustomerResponse createCustomer(CustomerRequest request) {
        User user = customerMapper.toEntity(request);
        user.setRole(Role.CUSTOMER);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);
        return toResponse(userRepository.save(user));
    }

    @Override
    public CustomerResponse updateCustomer(UUID id, CustomerRequest request) {
        User user = userRepository.findById(id)
                .filter(candidate -> candidate.getRole() == Role.CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));

        customerMapper.updateEntity(request, user);
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        return toResponse(userRepository.save(user));
    }

    @Override
    public void deleteCustomer(UUID id) {
        User user = userRepository.findById(id)
                .filter(candidate -> candidate.getRole() == Role.CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
        userRepository.delete(user);
    }

    private CustomerResponse toResponse(User user) {
        return CustomerResponse.builder()
                .id(user.getId())
                .name(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhoneNumber())
                .address(user.getAddress())
                .totalOrders(0L)
                .totalSpent(0D)
                .createdAt(user.getCreatedAt() == null ? null : user.getCreatedAt().format(DATE_FORMAT))
                .status(user.isEnabled() ? "ACTIVE" : "INACTIVE")
                .locked(!user.isEnabled())
                .build();
    }
}

