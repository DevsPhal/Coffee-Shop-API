package org.group1.coffeeshopapi.finance.service.impl;

import org.group1.coffeeshopapi.finance.dto.request.CreateExpenseRequest;
import org.group1.coffeeshopapi.finance.entity.Expense;
import org.group1.coffeeshopapi.finance.mapper.ExpenseMapper;
import org.group1.coffeeshopapi.finance.repository.ExpenseRepository;
import org.group1.coffeeshopapi.user.service.ActorLookupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the one real bit of logic in an otherwise thin CRUD service — defaulting expenseDate to
 * today when omitted — untested before. See ExpenseServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class ExpenseServiceImplTest {

    @Mock private ExpenseRepository expenseRepository;
    @Mock private ExpenseMapper expenseMapper;
    @Mock private ActorLookupService actorLookupService;
    @InjectMocks private ExpenseServiceImpl service;

    @Test
    void creatingAnExpenseWithNoDateDefaultsToToday() {
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(new CreateExpenseRequest("Rent", null, new BigDecimal("100.00"), null), UUID.randomUUID());

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(captor.capture());
        assertThat(captor.getValue().getExpenseDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void creatingAnExpenseWithAnExplicitDateKeepsIt() {
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));
        LocalDate backdated = LocalDate.of(2026, 1, 5);

        service.create(new CreateExpenseRequest("Rent", null, new BigDecimal("100.00"), backdated), UUID.randomUUID());

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(captor.capture());
        assertThat(captor.getValue().getExpenseDate()).isEqualTo(backdated);
    }
}
