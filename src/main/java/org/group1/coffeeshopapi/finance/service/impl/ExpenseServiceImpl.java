package org.group1.coffeeshopapi.finance.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.finance.dto.request.CreateExpenseRequest;
import org.group1.coffeeshopapi.finance.dto.request.UpdateExpenseRequest;
import org.group1.coffeeshopapi.finance.dto.response.ExpenseResponse;
import org.group1.coffeeshopapi.finance.entity.Expense;
import org.group1.coffeeshopapi.finance.mapper.ExpenseMapper;
import org.group1.coffeeshopapi.finance.repository.ExpenseRepository;
import org.group1.coffeeshopapi.finance.service.ExpenseService;
import org.group1.coffeeshopapi.user.dto.response.ActorSummary;
import org.group1.coffeeshopapi.user.service.ActorLookupService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseMapper expenseMapper;
    private final ActorLookupService actorLookupService;

    @Override
    @Transactional
    public ExpenseResponse create(CreateExpenseRequest request, UUID recordedBy) {
        Expense expense = new Expense();
        expense.setCategory(request.category());
        expense.setDescription(request.description());
        expense.setAmount(request.amount());
        expense.setExpenseDate(request.expenseDate() != null ? request.expenseDate() : LocalDate.now());
        expense.setRecordedBy(recordedBy);
        return toResponse(expenseRepository.save(expense));
    }

    @Override
    public ExpenseResponse getById(UUID id) {
        return toResponse(findById(id));
    }

    @Override
    public Page<ExpenseResponse> list(Pageable pageable) {
        Page<Expense> expenses = expenseRepository.findAllByOrderByExpenseDateDesc(pageable);

        Set<UUID> actorIds = new HashSet<>();
        for (Expense expense : expenses) {
            actorIds.add(expense.getRecordedBy());
        }
        Map<UUID, ActorSummary> actors = actorLookupService.resolveAll(actorIds);

        return expenses.map(expense -> expenseMapper.toResponse(expense, actors.get(expense.getRecordedBy())));
    }

    @Override
    @Transactional
    public ExpenseResponse update(UUID id, UpdateExpenseRequest request) {
        Expense expense = findById(id);
        if (request.category() != null) {
            expense.setCategory(request.category());
        }
        if (request.description() != null) {
            expense.setDescription(request.description());
        }
        if (request.amount() != null) {
            expense.setAmount(request.amount());
        }
        if (request.expenseDate() != null) {
            expense.setExpenseDate(request.expenseDate());
        }
        return toResponse(expenseRepository.save(expense));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        expenseRepository.delete(findById(id));
    }

    private ExpenseResponse toResponse(Expense expense) {
        return expenseMapper.toResponse(expense, actorLookupService.resolve(expense.getRecordedBy()));
    }

    private Expense findById(UUID id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
    }
}
