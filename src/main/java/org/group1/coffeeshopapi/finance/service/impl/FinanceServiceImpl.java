package org.group1.coffeeshopapi.finance.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.enums.PaymentMethod;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.finance.dto.response.FinanceSummaryResponse;
import org.group1.coffeeshopapi.finance.entity.Expense;
import org.group1.coffeeshopapi.finance.repository.ExpenseRepository;
import org.group1.coffeeshopapi.finance.service.FinanceService;
import org.group1.coffeeshopapi.inventory.entity.StockExpense;
import org.group1.coffeeshopapi.inventory.repository.StockExpenseRepository;
import org.group1.coffeeshopapi.order.entity.Order;
import org.group1.coffeeshopapi.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinanceServiceImpl implements FinanceService {

    private final OrderRepository orderRepository;
    private final ExpenseRepository expenseRepository;
    private final StockExpenseRepository stockExpenseRepository;

    @Override
    public FinanceSummaryResponse getDaily(LocalDate date) {
        return summarize(date, date.plusDays(1));
    }

    @Override
    public FinanceSummaryResponse getMonthly(int year, int month) {
        if (month < 1 || month > 12) {
            throw new InvalidOperationException("Month must be between 1 and 12");
        }
        LocalDate start = YearMonth.of(year, month).atDay(1);
        return summarize(start, start.plusMonths(1));
    }

    @Override
    public FinanceSummaryResponse getYearly(int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        return summarize(start, start.plusYears(1));
    }

    private FinanceSummaryResponse summarize(LocalDate startInclusive, LocalDate endExclusive) {
        LocalDateTime start = startInclusive.atStartOfDay();
        LocalDateTime end = endExclusive.atStartOfDay();

        List<Order> orders = orderRepository.findPaidInRange(start, end);
        BigDecimal cashIn = sumByMethod(orders, PaymentMethod.CASH);
        BigDecimal bakongIn = sumByMethod(orders, PaymentMethod.BAKONG);
        BigDecimal totalIn = cashIn.add(bakongIn);

        // Money out is two streams: manual entries (rent, wages) and auto-recorded stock costs.
        List<Expense> expenses = expenseRepository.findByExpenseDateGreaterThanEqualAndExpenseDateLessThan(
                startInclusive, endExclusive);
        BigDecimal manualOut = expenses.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<StockExpense> stockExpenses = stockExpenseRepository
                .findByExpenseDateGreaterThanEqualAndExpenseDateLessThan(startInclusive, endExclusive);
        BigDecimal stockPurchaseOut = stockExpenses.stream().map(StockExpense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOut = manualOut.add(stockPurchaseOut);

        return new FinanceSummaryResponse(
                startInclusive, endExclusive.minusDays(1), cashIn, bakongIn, totalIn,
                manualOut, stockPurchaseOut, totalOut, totalIn.subtract(totalOut));
    }

    private BigDecimal sumByMethod(List<Order> orders, PaymentMethod method) {
        return orders.stream()
                .filter(order -> order.getPaymentMethod() == method)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
