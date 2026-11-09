package org.group1.coffeeshopapi.order.service;

import org.group1.coffeeshopapi.common.enums.ReportPeriod;

import java.time.LocalDate;

public interface ReportExportService {

    // Workbook of the admin Reports page: finance for the chosen period, barista takings for `date`.
    // DAY reads finance for `date`; MONTH uses `year` and `month`; YEAR uses `year`.
    byte[] export(LocalDate date, ReportPeriod period, int year, int month);
}
