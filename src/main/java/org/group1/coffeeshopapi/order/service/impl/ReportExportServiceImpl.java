package org.group1.coffeeshopapi.order.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.enums.ReportPeriod;
import org.group1.coffeeshopapi.common.excel.BrandedSheet;
import org.group1.coffeeshopapi.common.excel.BrandedWorkbook;
import org.group1.coffeeshopapi.common.excel.ExcelText;
import org.group1.coffeeshopapi.common.excel.KeyValue;
import org.group1.coffeeshopapi.common.excel.TableColumn;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.finance.dto.response.FinanceSummaryResponse;
import org.group1.coffeeshopapi.finance.service.FinanceService;
import org.group1.coffeeshopapi.order.dto.response.AdminDailyReportResponse;
import org.group1.coffeeshopapi.order.dto.response.DailyReportResponse;
import org.group1.coffeeshopapi.order.service.ReportExportService;
import org.group1.coffeeshopapi.order.service.ReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportExportServiceImpl implements ReportExportService {

    private static final List<TableColumn> TAKINGS_COLUMNS = List.of(
            TableColumn.text("Barista"), TableColumn.integer("Orders"), TableColumn.money("Cash"),
            TableColumn.money("Bakong"), TableColumn.money("Total"));

    private final ReportService reportService;
    private final FinanceService financeService;

    @Override
    public byte[] export(LocalDate date, ReportPeriod period, int year, int month) {
        AdminDailyReportResponse report = reportService.getDailyReport(date);
        FinanceSummaryResponse finance = switch (period) {
            case DAY -> financeService.getDaily(date);
            case MONTH -> financeService.getMonthly(year, month);
            case YEAR -> financeService.getYearly(year);
        };

        try (BrandedWorkbook book = new BrandedWorkbook()) {
            writeSummary(book, period, finance, report);
            writeTakings(book, report);
            return book.toBytes();
        } catch (IOException e) {
            throw new InvalidOperationException("Unable to generate report: " + e.getMessage());
        }
    }

    private static void writeSummary(BrandedWorkbook book, ReportPeriod period,
                                     FinanceSummaryResponse finance, AdminDailyReportResponse report) {
        BrandedSheet sheet = book.sheet("Summary", "Finance Summary", List.of(
                "Finance period: " + ExcelText.period(period, finance.periodStart(), finance.periodEnd()),
                "Barista takings: " + ExcelText.date(report.date())));
        sheet.section("Finance");
        sheet.keyValues(List.of(
                KeyValue.text("Finance period", period.name() + " " + finance.periodStart() + " to " + finance.periodEnd()),
                KeyValue.money("Cash in", finance.cashIn()),
                KeyValue.money("Bakong in", finance.bakongIn()),
                KeyValue.money("Money in", finance.totalIn()),
                KeyValue.money("Expenses", finance.manualExpensesOut()),
                KeyValue.money("Stock purchases", finance.stockPurchasesOut()),
                KeyValue.money("Money out", finance.totalOut()),
                KeyValue.money("Profit", finance.profit())));
        sheet.blank();
        sheet.section("Barista takings");
        sheet.keyValues(List.of(
                KeyValue.text("Barista takings date", report.date().toString()),
                KeyValue.count("Orders on that day", report.totalOrders()),
                KeyValue.money("Takings on that day", report.grandTotal())));
    }

    private static void writeTakings(BrandedWorkbook book, AdminDailyReportResponse report) {
        BrandedSheet sheet = book.sheet("Barista Takings", "Barista Takings", List.of(
                "Date: " + ExcelText.date(report.date()),
                "Orders: " + report.totalOrders() + " · Takings: " + ExcelText.usd(report.grandTotal())));

        List<Object[]> rows = new ArrayList<>();
        long baristaOrders = 0;
        BigDecimal baristaCash = BigDecimal.ZERO;
        BigDecimal baristaBakong = BigDecimal.ZERO;
        for (DailyReportResponse barista : report.baristas()) {
            rows.add(new Object[] {ExcelText.displayName(barista.baristaName()), barista.totalOrders(),
                    barista.cashTotal(), barista.bakongTotal(), barista.grandTotal()});
            baristaOrders += barista.totalOrders();
            baristaCash = baristaCash.add(barista.cashTotal());
            baristaBakong = baristaBakong.add(barista.bakongTotal());
        }

        // Paid orders no staff member handled (customer self-service Bakong) count in the day's total
        // but in no barista's row; list them so the columns add up to the total row.
        long selfServiceOrders = report.totalOrders() - baristaOrders;
        BigDecimal selfServiceCash = report.cashTotal().subtract(baristaCash);
        BigDecimal selfServiceBakong = report.bakongTotal().subtract(baristaBakong);
        if (selfServiceOrders != 0 || selfServiceCash.signum() != 0 || selfServiceBakong.signum() != 0) {
            rows.add(new Object[] {"Self-service (no barista)", selfServiceOrders, selfServiceCash,
                    selfServiceBakong, selfServiceCash.add(selfServiceBakong)});
        }

        sheet.table(TAKINGS_COLUMNS, rows, new Object[] {"Total", report.totalOrders(), report.cashTotal(),
                report.bakongTotal(), report.grandTotal()});
    }
}
