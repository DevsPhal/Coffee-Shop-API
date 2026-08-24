package org.group1.coffeeshopapi.finance.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.constant.AppConstant;
import org.group1.coffeeshopapi.common.response.ApiResponse;
import org.group1.coffeeshopapi.common.response.PageResponse;
import org.group1.coffeeshopapi.common.security.CurrentActor;
import org.group1.coffeeshopapi.common.util.PageUtil;
import org.group1.coffeeshopapi.finance.dto.request.CreateExpenseRequest;
import org.group1.coffeeshopapi.finance.dto.request.UpdateExpenseRequest;
import org.group1.coffeeshopapi.finance.dto.response.ExpenseResponse;
import org.group1.coffeeshopapi.finance.service.ExpenseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/expenses")
@RequiredArgsConstructor
@Tag(name = "Admin Expenses", description = "Admin only: track money out (rent, supplies, wages, ...)")
@SecurityRequirement(name = "bearerAuth")
public class AdminExpenseController {

    private final ExpenseService expenseService;
    private final CurrentActor currentActor;

    @PostMapping
    public ResponseEntity<ApiResponse<ExpenseResponse>> create(@Valid @RequestBody CreateExpenseRequest request) {
        ExpenseResponse response = expenseService.create(request, currentActor.id());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(HttpStatus.CREATED, "Expense recorded successfully.", response));
    }

    @GetMapping
    public ApiResponse<PageResponse<ExpenseResponse>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                PageResponse.of(expenseService.list(PageUtil.buildPageable(page, size))));
    }

    @GetMapping("/{id}")
    public ApiResponse<ExpenseResponse> getById(@PathVariable UUID id) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, expenseService.getById(id));
    }

    @PatchMapping("/{id}")
    public ApiResponse<ExpenseResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateExpenseRequest request) {
        return ApiResponse.of(HttpStatus.OK, "Expense updated successfully.", expenseService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        expenseService.delete(id);
        return ApiResponse.of(HttpStatus.OK, "Expense deleted successfully.", null);
    }
}
