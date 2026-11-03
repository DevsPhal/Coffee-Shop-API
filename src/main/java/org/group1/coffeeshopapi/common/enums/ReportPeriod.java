package org.group1.coffeeshopapi.common.enums;

import java.time.LocalDate;

public enum ReportPeriod {
    DAY, MONTH, YEAR;

    public LocalDate start(LocalDate date) {
        return switch (this) {
            case DAY -> date;
            case MONTH -> date.withDayOfMonth(1);
            case YEAR -> date.withDayOfYear(1);
        };
    }

    // Exclusive: the first day after the period.
    public LocalDate end(LocalDate date) {
        LocalDate start = start(date);
        return switch (this) {
            case DAY -> start.plusDays(1);
            case MONTH -> start.plusMonths(1);
            case YEAR -> start.plusYears(1);
        };
    }
}
