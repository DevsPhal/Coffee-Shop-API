package org.group1.coffeeshopapi.customer.controller;

import java.util.UUID;

import org.group1.coffeeshopapi.common.responses.PaginatedResponse;
import org.group1.coffeeshopapi.common.utils.PageUtil;
import org.group1.coffeeshopapi.customer.dto.request.CustomerRequest;
import org.group1.coffeeshopapi.customer.dto.response.CustomerResponse;
import org.group1.coffeeshopapi.customer.service.CustomerService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/customer")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public PaginatedResponse<CustomerResponse> getAllCustomers(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction) {
        Pageable pageable = PageUtil.buildPageable(page, size, sortBy, direction);
        return customerService.getAllCustomers(pageable);
    }

    @GetMapping("/{id}")
    public CustomerResponse getCustomerById(@PathVariable UUID id) {
        return customerService.getCustomerById(id);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public CustomerResponse createCustomer(@RequestBody CustomerRequest request) {
        return customerService.createCustomer(request);
    }

    @PutMapping("/{id}")
    public CustomerResponse updateCustomer(
            @PathVariable UUID id,
            @RequestBody CustomerRequest request) {
        return customerService.updateCustomer(id, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void deleteCustomer(@PathVariable UUID id) {
        customerService.deleteCustomer(id);
    }
}
