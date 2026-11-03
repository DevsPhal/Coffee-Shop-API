package org.group1.coffeeshopapi.inventory.service;

import org.group1.coffeeshopapi.common.enums.ReportPeriod;
import org.group1.coffeeshopapi.inventory.dto.response.InventoryReportResponse;

import java.time.LocalDate;

public interface InventoryReportService {
    // The day, month or year that contains `date`.
    InventoryReportResponse summary(ReportPeriod period, LocalDate date);

    byte[] exportSummary(ReportPeriod period, LocalDate date);
}
