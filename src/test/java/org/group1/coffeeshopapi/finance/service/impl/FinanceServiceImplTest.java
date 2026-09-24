package org.group1.coffeeshopapi.finance.service.impl;

import org.group1.coffeeshopapi.common.enums.PaymentMethod;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.finance.dto.response.FinanceSummaryResponse;
import org.group1.coffeeshopapi.finance.entity.Expense;
import org.group1.coffeeshopapi.finance.repository.ExpenseRepository;
import org.group1.coffeeshopapi.inventory.entity.StockExpense;
import org.group1.coffeeshopapi.inventory.repository.StockExpenseRepository;
import org.group1.coffeeshopapi.order.entity.Order;
import org.group1.coffeeshopapi.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

// Covers the daily/monthly/yearly money-in vs. money-out summary math behind the shop's
// financial reporting.
@ExtendWith(MockitoExtension.class)
class FinanceServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private StockExpenseRepository stockExpenseRepository;
    @InjectMocks private FinanceServiceImpl service;

    @Test
    void dailySummaryAddsCashAndBakongSalesAndSubtractsBothExpenseStreams() {
        LocalDate date = LocalDate.of(2026, 3, 10);
        when(orderRepository.findPaidInRange(any(), any())).thenReturn(List.of(
                order(PaymentMethod.CASH, "20.00"),
                order(PaymentMethod.CASH, "5.50"),
                order(PaymentMethod.BAKONG, "14.00")));
        when(expenseRepository.findByExpenseDateGreaterThanEqualAndExpenseDateLessThan(any(), any()))
                .thenReturn(List.of(expense("10.00")));
        when(stockExpenseRepository.findByExpenseDateGreaterThanEqualAndExpenseDateLessThan(any(), any()))
                .thenReturn(List.of(stockExpense("8.00"), stockExpense("2.00")));

        FinanceSummaryResponse summary = service.getDaily(date);

        assertThat(summary.cashIn()).isEqualByComparingTo("25.50");
        assertThat(summary.bakongIn()).isEqualByComparingTo("14.00");
        assertThat(summary.totalIn()).isEqualByComparingTo("39.50");
        assertThat(summary.manualExpensesOut()).isEqualByComparingTo("10.00");
        assertThat(summary.stockPurchasesOut()).isEqualByComparingTo("10.00");
        assertThat(summary.totalOut()).isEqualByComparingTo("20.00");
        assertThat(summary.profit()).isEqualByComparingTo("19.50");
        assertThat(summary.periodStart()).isEqualTo(date);
        assertThat(summary.periodEnd()).isEqualTo(date);
    }

    @Test
    void monthlySummaryRejectsAnOutOfRangeMonth() {
        assertThatThrownBy(() -> service.getMonthly(2026, 13)).isInstanceOf(InvalidOperationException.class);
        assertThatThrownBy(() -> service.getMonthly(2026, 0)).isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void monthlySummarySpansTheWholeCalendarMonth() {
        when(orderRepository.findPaidInRange(any(), any())).thenReturn(List.of());
        when(expenseRepository.findByExpenseDateGreaterThanEqualAndExpenseDateLessThan(any(), any())).thenReturn(List.of());
        when(stockExpenseRepository.findByExpenseDateGreaterThanEqualAndExpenseDateLessThan(any(), any())).thenReturn(List.of());

        FinanceSummaryResponse summary = service.getMonthly(2026, 2);

        assertThat(summary.periodStart()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(summary.periodEnd()).isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    void yearlySummarySpansTheWholeCalendarYear() {
        when(orderRepository.findPaidInRange(any(), any())).thenReturn(List.of());
        when(expenseRepository.findByExpenseDateGreaterThanEqualAndExpenseDateLessThan(any(), any())).thenReturn(List.of());
        when(stockExpenseRepository.findByExpenseDateGreaterThanEqualAndExpenseDateLessThan(any(), any())).thenReturn(List.of());

        FinanceSummaryResponse summary = service.getYearly(2026);

        assertThat(summary.periodStart()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(summary.periodEnd()).isEqualTo(LocalDate.of(2026, 12, 31));
    }

    private Order order(PaymentMethod method, String amount) {
        Order order = new Order();
        order.setPaymentMethod(method);
        order.setTotalAmount(new BigDecimal(amount));
        return order;
    }

    private Expense expense(String amount) {
        Expense expense = new Expense();
        expense.setAmount(new BigDecimal(amount));
        return expense;
    }

    private StockExpense stockExpense(String amount) {
        StockExpense expense = new StockExpense();
        expense.setAmount(new BigDecimal(amount));
        return expense;
    }
}
