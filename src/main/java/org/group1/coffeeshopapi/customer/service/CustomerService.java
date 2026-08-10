package org.group1.coffeeshopapi.customer.service;

import java.util.UUID;

import org.group1.coffeeshopapi.common.responses.PageResponse;
import org.group1.coffeeshopapi.customer.dto.request.CustomerRequest;
import org.group1.coffeeshopapi.customer.dto.response.CustomerResponse;
import org.springframework.data.domain.Pageable;

public interface CustomerService {

    PageResponse<CustomerResponse> getAllCustomers(Pageable pageable);

    CustomerResponse getCustomerById(UUID id);

    CustomerResponse createCustomer(CustomerRequest request);

    CustomerResponse updateCustomer(UUID id, CustomerRequest request);

    void deleteCustomer(UUID id);
}
