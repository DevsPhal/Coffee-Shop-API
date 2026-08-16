package org.group1.coffeeshopapi.customer.service.impl;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group1.coffeeshopapi.user.entity.User;
import org.group1.coffeeshopapi.user.repository.UserRepository;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.responses.PaginatedResponse;
import org.group1.coffeeshopapi.common.utils.PageUtil;
import org.group1.coffeeshopapi.customer.dto.request.CustomerRequest;
import org.group1.coffeeshopapi.customer.dto.response.CustomerResponse;
import org.group1.coffeeshopapi.customer.mapper.CustomerMapper;
import org.group1.coffeeshopapi.customer.service.CustomerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@AllArgsConstructor
@Transactional

public class CustomerServiceImpl implements CustomerService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomerMapper customerMapper;

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<CustomerResponse> getAllCustomers(Pageable pageable) {
        Page<CustomerResponse> page = userRepository.findByRole(Role.CUSTOMER, pageable).map(customerMapper::toResponse);
        return PageUtil.toPaginatedResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(UUID id) {
        User user = userRepository.findById(id)
                .filter(candidate -> candidate.getRole() == Role.CUSTOMER)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found: " + id));
        return customerMapper.toResponse(user);
    }

    @Override
    public CustomerResponse createCustomer(CustomerRequest request) {
        validateUniqueCustomer(request, null);
        log.info("Customer before saving: {}", request);
        User user = customerMapper.toEntity(request);
        user.setRole(Role.CUSTOMER);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);
        User saved = userRepository.save(user);
        log.info("Customer after saving: {}", saved.getId());
        return customerMapper.toResponse(saved);
    }

    @Override
    public CustomerResponse updateCustomer(UUID id, CustomerRequest request) {
        User user = userRepository.findById(id)
                .filter(candidate -> candidate.getRole() == Role.CUSTOMER)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found: " + id));

        validateUniqueCustomer(request, id);
        customerMapper.updateEntity(request, user);
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        return customerMapper.toResponse(userRepository.save(user));
    }

    @Override
    public void deleteCustomer(UUID id) {
        User user = userRepository.findById(id)
                .filter(candidate -> candidate.getRole() == Role.CUSTOMER)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found: " + id));
        userRepository.delete(user);
    }

    private void validateUniqueCustomer(CustomerRequest request, UUID currentId) {
        if (request.getPhone() != null && !request.getPhone().isBlank()
                && (currentId == null
                ? userRepository.existsByPhoneNumber(request.getPhone())
                : userRepository.existsByPhoneNumberAndIdNot(request.getPhone(), currentId))) {
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
