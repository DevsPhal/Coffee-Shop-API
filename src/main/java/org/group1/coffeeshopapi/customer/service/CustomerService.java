package org.group1.coffeeshopapi.customer.service;

import java.util.List;

import org.group1.coffeeshopapi.customer.dto.request.CustomerRequest;
import org.group1.coffeeshopapi.customer.dto.response.CustomerResponse;

public interface CustomerService {

    List<CustomerResponse> getAllCustomers();

    CustomerResponse getCustomerById(Long id);

    CustomerResponse createCustomer(CustomerRequest request);

    CustomerResponse updateCustomer(Long id, CustomerRequest request);

    void deleteCustomer(Long id);
}
