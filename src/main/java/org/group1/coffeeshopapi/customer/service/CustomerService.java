package org.group1.coffeeshopapi.customer.service;

import java.util.UUID;

import org.group1.coffeeshopapi.common.responses.PaginatedResponse;
import org.group1.coffeeshopapi.customer.dto.request.CustomerRequest;
import org.group1.coffeeshopapi.customer.dto.response.CustomerResponse;
import org.springframework.data.domain.Pageable;

public interface CustomerService {

    PaginatedResponse<CustomerResponse> getAllCustomers(Pageable pageable);

    CustomerResponse getCustomerById(UUID id);

    CustomerResponse createCustomer(CustomerRequest request);

    CustomerResponse updateCustomer(UUID id, CustomerRequest request);

    void deleteCustomer(UUID id);
}
