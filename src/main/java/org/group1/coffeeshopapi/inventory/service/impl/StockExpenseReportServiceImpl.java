package org.group1.coffeeshopapi.inventory.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.inventory.entity.StockExpense;
import org.group1.coffeeshopapi.inventory.repository.StockExpenseRepository;
import org.group1.coffeeshopapi.inventory.service.StockExpenseReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockExpenseReportServiceImpl implements StockExpenseReportService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String[] HEADERS = {"Date", "Product", "Quantity", "Unit Cost", "Amount"};

    private final StockExpenseRepository stockExpenseRepository;

    @Override
    public byte[] generateMonthlyReport(YearMonth month) {
        List<StockExpense> expenses = stockExpenseRepository.findByExpenseDateGreaterThanEqualAndExpenseDateLessThan(
                month.atDay(1), month.plusMonths(1).atDay(1));

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Stock Expenses " + month);

            CellStyle headerStyle = headerStyle(workbook);
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            CellStyle moneyStyle = moneyStyle(workbook);
            BigDecimal total = BigDecimal.ZERO;
            int rowIndex = 1;
            for (StockExpense expense : expenses) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(expense.getExpenseDate().format(DATE_FORMAT));
                row.createCell(1).setCellValue(expense.getProduct().getName());
                row.createCell(2).setCellValue(expense.getQuantity().doubleValue());
                writeMoneyCell(row.createCell(3), expense.getUnitCost(), moneyStyle);
                writeMoneyCell(row.createCell(4), expense.getAmount(), moneyStyle);
                total = total.add(expense.getAmount());
            }

            Row totalRow = sheet.createRow(rowIndex + 1);
            Cell totalLabelCell = totalRow.createCell(3);
            totalLabelCell.setCellValue("Total");
            totalLabelCell.setCellStyle(headerStyle);
            writeMoneyCell(totalRow.createCell(4), total, moneyStyle);

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new InvalidOperationException("Unable to generate expense report: " + e.getMessage());
        }
    }

    private void writeMoneyCell(Cell cell, BigDecimal amount, CellStyle style) {
        cell.setCellValue(amount.doubleValue());
        cell.setCellStyle(style);
    }

    private CellStyle headerStyle(Workbook workbook) {
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(boldFont);
        return style;
    }

    private CellStyle moneyStyle(Workbook workbook) {
        DataFormat format = workbook.createDataFormat();
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(format.getFormat("$#,##0.00"));
        return style;
    }
}
