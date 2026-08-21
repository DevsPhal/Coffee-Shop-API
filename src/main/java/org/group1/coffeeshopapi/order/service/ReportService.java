package org.group1.coffeeshopapi.order.service;

import org.group1.coffeeshopapi.order.dto.response.AdminDailyReportResponse;
import org.group1.coffeeshopapi.order.dto.response.DailyReportResponse;

import java.time.LocalDate;
import java.util.UUID;

public interface ReportService {

    DailyReportResponse getOwnDailyReport(UUID baristaId, LocalDate date);

    AdminDailyReportResponse getDailyReport(LocalDate date);
}
