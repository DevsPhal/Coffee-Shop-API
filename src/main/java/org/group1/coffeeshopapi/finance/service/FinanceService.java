package org.group1.coffeeshopapi.finance.service;

import org.group1.coffeeshopapi.finance.dto.response.FinanceSummaryResponse;

import java.time.LocalDate;

public interface FinanceService {
    FinanceSummaryResponse getDaily(LocalDate date);
    FinanceSummaryResponse getMonthly(int year, int month);
    FinanceSummaryResponse getYearly(int year);
}
