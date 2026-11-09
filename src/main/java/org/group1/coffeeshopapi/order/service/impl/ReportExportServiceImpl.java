package org.group1.coffeeshopapi.order.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.group1.coffeeshopapi.common.enums.ReportPeriod;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.finance.dto.response.FinanceSummaryResponse;
import org.group1.coffeeshopapi.finance.service.FinanceService;
import org.group1.coffeeshopapi.order.dto.response.AdminDailyReportResponse;
import org.group1.coffeeshopapi.order.dto.response.DailyReportResponse;
import org.group1.coffeeshopapi.order.service.ReportExportService;
import org.group1.coffeeshopapi.order.service.ReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportExportServiceImpl implements ReportExportService {

    private static final String[] TAKINGS_HEADERS = {"Barista", "Orders", "Cash", "Bakong", "Total"};

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

        try (Workbook workbook = new XSSFWorkbook()) {
            Styles styles = new Styles(workbook);
            writeSummary(workbook.createSheet("Summary"), styles, period, finance, report);
            writeTakings(workbook.createSheet("Barista Takings"), styles, report);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new InvalidOperationException("Unable to generate report: " + e.getMessage());
        }
    }

    private static void writeSummary(Sheet sheet, Styles styles, ReportPeriod period,
                                     FinanceSummaryResponse finance, AdminDailyReportResponse report) {
        Object[][] lines = {
                {"Finance period", period.name() + " " + finance.periodStart() + " to " + finance.periodEnd()},
                {"Cash in", finance.cashIn()},
                {"Bakong in", finance.bakongIn()},
                {"Money in", finance.totalIn()},
                {"Expenses", finance.manualExpensesOut()},
                {"Stock purchases", finance.stockPurchasesOut()},
                {"Money out", finance.totalOut()},
                {"Profit", finance.profit()},
                null,
                {"Barista takings date", report.date().toString()},
                {"Orders on that day", report.totalOrders()},
                {"Takings on that day", report.grandTotal()}};
        for (int i = 0; i < lines.length; i++) {
            if (lines[i] == null) {
                continue;
            }
            Row row = sheet.createRow(i);
            Cell label = row.createCell(0);
            label.setCellValue((String) lines[i][0]);
            label.setCellStyle(styles.bold);
            write(row.createCell(1), lines[i][1], styles.money);
        }
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private static void writeTakings(Sheet sheet, Styles styles, AdminDailyReportResponse report) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < TAKINGS_HEADERS.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(TAKINGS_HEADERS[i]);
            cell.setCellStyle(styles.bold);
        }

        int rowIndex = 1;
        long baristaOrders = 0;
        BigDecimal baristaCash = BigDecimal.ZERO;
        BigDecimal baristaBakong = BigDecimal.ZERO;
        for (DailyReportResponse barista : report.baristas()) {
            writeTakingsRow(sheet.createRow(rowIndex++), styles, false, displayName(barista.baristaName()),
                    barista.totalOrders(), barista.cashTotal(), barista.bakongTotal(), barista.grandTotal());
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
            writeTakingsRow(sheet.createRow(rowIndex++), styles, false, "Self-service (no barista)",
                    selfServiceOrders, selfServiceCash, selfServiceBakong, selfServiceCash.add(selfServiceBakong));
        }

        writeTakingsRow(sheet.createRow(rowIndex), styles, true, "Total",
                report.totalOrders(), report.cashTotal(), report.bakongTotal(), report.grandTotal());

        for (int i = 0; i < TAKINGS_HEADERS.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private static void writeTakingsRow(Row row, Styles styles, boolean total, String name, long orders,
                                        BigDecimal cash, BigDecimal bakong, BigDecimal grandTotal) {
        Cell label = row.createCell(0);
        label.setCellValue(name);
        Cell count = row.createCell(1);
        count.setCellValue(orders);
        write(row.createCell(2), cash, total ? styles.boldMoney : styles.money);
        write(row.createCell(3), bakong, total ? styles.boldMoney : styles.money);
        write(row.createCell(4), grandTotal, styles.boldMoney);
        label.setCellStyle(styles.bold);
        if (total) {
            count.setCellStyle(styles.bold);
        }
    }

    // Same capitalisation the Reports page shows ("sophal" → "Sophal").
    private static String displayName(String name) {
        if (name == null || name.isBlank()) {
            return "Unknown";
        }
        StringBuilder out = new StringBuilder(name.length());
        boolean wordStart = true;
        for (char c : name.toCharArray()) {
            out.append(wordStart ? Character.toUpperCase(c) : c);
            wordStart = Character.isWhitespace(c);
        }
        return out.toString();
    }

    private static void write(Cell cell, Object value, CellStyle moneyStyle) {
        if (value instanceof BigDecimal number) {
            cell.setCellValue(number.doubleValue());
            cell.setCellStyle(moneyStyle);
        } else if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else if (value != null) {
            cell.setCellValue(value.toString());
        }
    }

    private static final class Styles {
        private final CellStyle bold;
        private final CellStyle money;
        private final CellStyle boldMoney;

        private Styles(Workbook workbook) {
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            short moneyFormat = workbook.createDataFormat().getFormat("$#,##0.00");

            bold = workbook.createCellStyle();
            bold.setFont(boldFont);
            money = workbook.createCellStyle();
            money.setDataFormat(moneyFormat);
            boldMoney = workbook.createCellStyle();
            boldMoney.setFont(boldFont);
            boldMoney.setDataFormat(moneyFormat);
        }
    }
}
