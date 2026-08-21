package org.group1.coffeeshopapi.finance.service;

import org.group1.coffeeshopapi.finance.dto.request.CreateExpenseRequest;
import org.group1.coffeeshopapi.finance.dto.request.UpdateExpenseRequest;
import org.group1.coffeeshopapi.finance.dto.response.ExpenseResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ExpenseService {
    ExpenseResponse create(CreateExpenseRequest request, UUID recordedBy);
    ExpenseResponse getById(UUID id);
    Page<ExpenseResponse> list(Pageable pageable);
    ExpenseResponse update(UUID id, UpdateExpenseRequest request);
    void delete(UUID id);
}
