package org.group1.coffeeshopapi.order.service.impl;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.group1.coffeeshopapi.common.enums.ReportPeriod;
import org.group1.coffeeshopapi.common.excel.BrandedSheet;
import org.group1.coffeeshopapi.common.excel.BrandingAssertions;
import org.group1.coffeeshopapi.finance.dto.response.FinanceSummaryResponse;
import org.group1.coffeeshopapi.finance.service.FinanceService;
import org.group1.coffeeshopapi.order.dto.response.AdminDailyReportResponse;
import org.group1.coffeeshopapi.order.dto.response.DailyReportResponse;
import org.group1.coffeeshopapi.order.service.ReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.group1.coffeeshopapi.common.excel.BrandingAssertions.rowStartingWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportExportServiceImplTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 9);

    @Mock private ReportService reportService;
    @Mock private FinanceService financeService;
    @InjectMocks private ReportExportServiceImpl service;

    @Test
    void exportsFinanceSummaryAndBaristaTakingsWithASelfServiceRowSoColumnsAddUp() throws IOException {
        when(reportService.getDailyReport(DATE)).thenReturn(new AdminDailyReportResponse(DATE, 3,
                new BigDecimal("3.50"), new BigDecimal("2.69"), new BigDecimal("6.19"), List.of(
                barista("sophal", 2, "3.50", "1.00"))));
        when(financeService.getDaily(DATE)).thenReturn(finance(DATE, DATE));

        try (XSSFWorkbook workbook = BrandingAssertions.open(service.export(DATE, ReportPeriod.DAY, 2026, 11))) {
            BrandingAssertions.assertEverySheetBranded(workbook);

            Sheet summary = workbook.getSheet("Summary");
            assertThat(BrandingAssertions.text(summary, BrandedSheet.TITLE_ROW)).isEqualTo("Finance Summary");
            assertThat(BrandingAssertions.text(summary, BrandedSheet.GENERATED_ROW + 1))
                    .isEqualTo("Finance period: Daily, 9 Nov 2026");
            assertThat(rowStartingWith(summary, "Finance period").getCell(1).getStringCellValue())
                    .isEqualTo("DAY 2026-11-09 to 2026-11-09");
            assertThat(rowStartingWith(summary, "Money in").getCell(1).getNumericCellValue()).isEqualTo(6.19);
            assertThat(rowStartingWith(summary, "Profit").getCell(1).getNumericCellValue()).isEqualTo(4.19);
            assertThat(rowStartingWith(summary, "Profit").getCell(1).getCellStyle().getDataFormatString())
                    .contains("$#,##0.00");
            assertThat(rowStartingWith(summary, "Orders on that day").getCell(1).getNumericCellValue()).isEqualTo(3);

            Sheet takings = workbook.getSheet("Barista Takings");
            Row header = rowStartingWith(takings, "Barista");
            int first = header.getRowNum() + 1;
            assertThat(text(header)).containsExactly("Barista", "Orders", "Cash", "Bakong", "Total");
            assertThat(takings.getRow(first).getCell(0).getStringCellValue()).isEqualTo("Sophal");
            assertThat(takings.getRow(first).getCell(4).getNumericCellValue()).isEqualTo(4.50);

            Row selfService = takings.getRow(first + 1);
            assertThat(selfService.getCell(0).getStringCellValue()).isEqualTo("Self-service (no barista)");
            assertThat(selfService.getCell(1).getNumericCellValue()).isEqualTo(1);
            assertThat(selfService.getCell(2).getNumericCellValue()).isZero();
            assertThat(selfService.getCell(3).getNumericCellValue()).isEqualTo(1.69);

            Row total = takings.getRow(first + 2);
            assertThat(total.getCell(0).getStringCellValue()).isEqualTo("Total");
            assertThat(total.getCell(1).getNumericCellValue()).isEqualTo(3);
            assertThat(total.getCell(4).getNumericCellValue()).isEqualTo(6.19);
            assertThat(takings.getLastRowNum()).isEqualTo(first + 2);
            assertThat(takings.getRepeatingRows().getFirstRow()).isEqualTo(header.getRowNum());
        }
    }

    @Test
    void omitsTheSelfServiceRowWhenEveryOrderHasABarista() throws IOException {
        when(reportService.getDailyReport(DATE)).thenReturn(new AdminDailyReportResponse(DATE, 2,
                new BigDecimal("3.50"), new BigDecimal("1.00"), new BigDecimal("4.50"), List.of(
                barista("sophal", 2, "3.50", "1.00"))));
        when(financeService.getMonthly(2026, 11)).thenReturn(finance(DATE.withDayOfMonth(1), LocalDate.of(2026, 11, 30)));

        try (XSSFWorkbook workbook = BrandingAssertions.open(service.export(DATE, ReportPeriod.MONTH, 2026, 11))) {
            BrandingAssertions.assertEverySheetBranded(workbook);
            Sheet takings = workbook.getSheet("Barista Takings");
            int first = rowStartingWith(takings, "Barista").getRowNum() + 1;
            assertThat(takings.getRow(first + 1).getCell(0).getStringCellValue()).isEqualTo("Total");
            assertThat(takings.getLastRowNum()).isEqualTo(first + 1);
            Sheet summary = workbook.getSheet("Summary");
            assertThat(rowStartingWith(summary, "Finance period").getCell(1).getStringCellValue())
                    .isEqualTo("MONTH 2026-11-01 to 2026-11-30");
            assertThat(BrandingAssertions.text(summary, BrandedSheet.GENERATED_ROW + 1))
                    .isEqualTo("Finance period: Monthly, 1 Nov 2026 to 30 Nov 2026");
        }
        verify(financeService).getMonthly(2026, 11);
        verifyNoMoreInteractions(financeService);
    }

    private static DailyReportResponse barista(String name, long orders, String cash, String bakong) {
        BigDecimal cashTotal = new BigDecimal(cash);
        BigDecimal bakongTotal = new BigDecimal(bakong);
        return new DailyReportResponse(UUID.randomUUID(), name, DATE, orders, cashTotal, bakongTotal,
                cashTotal.add(bakongTotal));
    }

    private static FinanceSummaryResponse finance(LocalDate start, LocalDate end) {
        return new FinanceSummaryResponse(start, end, new BigDecimal("3.50"), new BigDecimal("2.69"),
                new BigDecimal("6.19"), new BigDecimal("1.50"), new BigDecimal("0.50"), new BigDecimal("2.00"),
                new BigDecimal("4.19"));
    }

    private static List<String> text(Row row) {
        return List.of(row.getCell(0).getStringCellValue(), row.getCell(1).getStringCellValue(),
                row.getCell(2).getStringCellValue(), row.getCell(3).getStringCellValue(),
                row.getCell(4).getStringCellValue());
    }
}
