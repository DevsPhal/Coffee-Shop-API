package org.group1.coffeeshopapi.inventory.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.group1.coffeeshopapi.common.enums.QuantityUnit;
import org.group1.coffeeshopapi.common.enums.ReportPeriod;
import org.group1.coffeeshopapi.common.enums.StockMovementReason;
import org.group1.coffeeshopapi.common.enums.StockMovementType;
import org.group1.coffeeshopapi.common.enums.WasteReason;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.inventory.dto.response.InventoryReportResponse;
import org.group1.coffeeshopapi.inventory.dto.response.InventoryReportRow;
import org.group1.coffeeshopapi.inventory.entity.Inventory;
import org.group1.coffeeshopapi.inventory.entity.StockExpense;
import org.group1.coffeeshopapi.inventory.repository.InventoryRepository;
import org.group1.coffeeshopapi.inventory.repository.StockBatchRepository;
import org.group1.coffeeshopapi.inventory.repository.StockExpenseRepository;
import org.group1.coffeeshopapi.inventory.repository.StockMovementRepository;
import org.group1.coffeeshopapi.inventory.service.InventoryReportService;
import org.group1.coffeeshopapi.product.entity.Product;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryReportServiceImpl implements InventoryReportService {

    private static final int MONEY_SCALE = 2;

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StockExpenseRepository stockExpenseRepository;
    private final StockBatchRepository stockBatchRepository;

    @Override
    public InventoryReportResponse summary(ReportPeriod period, LocalDate date) {
        LocalDate from = period.start(date);
        LocalDate to = period.end(date);

        Map<UUID, Totals> totals = new HashMap<>();
        long movementCount = 0;
        long saleCount = 0;
        long wasteCount = 0;
        for (StockMovementRepository.MovementTotals row
                : stockMovementRepository.totalsBetween(from.atStartOfDay(), to.atStartOfDay())) {
            Totals product = totals.computeIfAbsent(row.getProductId(), id -> new Totals());
            BigDecimal quantity = orZero(row.getQuantity());
            BigDecimal cost = orZero(row.getTotalCost());
            movementCount += row.getMovements();
            switch (reasonOf(row.getReason(), row.getType())) {
                case PURCHASE -> product.purchased = product.purchased.add(quantity);
                case SALE -> {
                    product.sold = product.sold.add(quantity);
                    product.salesCost = product.salesCost.add(cost);
                    saleCount += row.getMovements();
                }
                case RETURN -> {
                    product.returned = product.returned.add(quantity);
                    product.returnValue = product.returnValue.add(cost);
                }
                case WASTE -> {
                    product.wasted = product.wasted.add(quantity);
                    product.wasteCost = product.wasteCost.add(cost);
                    wasteCount += row.getMovements();
                }
                case ADJUSTMENT -> {
                    if (row.getType() == StockMovementType.STOCK_IN) {
                        product.adjustedIn = product.adjustedIn.add(quantity);
                        product.adjustmentValue = product.adjustmentValue.add(cost);
                    } else {
                        product.adjustedOut = product.adjustedOut.add(quantity);
                        product.adjustmentValue = product.adjustmentValue.subtract(cost);
                    }
                }
            }
        }

        // Purchases come from the expense records, which hold the cost of every stock-in, old ones included.
        List<StockExpense> expenses = stockExpenseRepository.findByExpenseDateGreaterThanEqualAndExpenseDateLessThan(from, to);
        for (StockExpense expense : expenses) {
            Totals product = totals.computeIfAbsent(expense.getProduct().getId(), id -> new Totals());
            product.purchaseCost = product.purchaseCost.add(expense.getAmount());
        }

        Map<UUID, BigDecimal> stockValues = new HashMap<>();
        for (StockBatchRepository.StockValue value : stockBatchRepository.valueOnHand()) {
            stockValues.put(value.getProductId(), orZero(value.getValue()));
        }

        Map<WasteReason, BigDecimal> wasteByReason = new EnumMap<>(WasteReason.class);
        for (StockMovementRepository.WasteTotals row
                : stockMovementRepository.wasteBetween(from.atStartOfDay(), to.atStartOfDay())) {
            WasteReason reason = row.getWasteReason() != null ? row.getWasteReason() : WasteReason.OTHER;
            wasteByReason.merge(reason, money(row.getTotalCost()), BigDecimal::add);
        }

        long lowStock = 0;
        long outOfStock = 0;
        List<InventoryReportRow> rows = new ArrayList<>();
        for (Inventory inventory : inventoryRepository.findAll()) {
            Product product = inventory.getProduct();
            BigDecimal onHand = inventory.getQuantityOnHand();
            if (onHand.signum() <= 0) {
                outOfStock++;
            }
            if (onHand.compareTo(inventory.getReorderLevel()) <= 0) {
                lowStock++;
            }
            Totals t = totals.getOrDefault(product.getId(), new Totals());
            rows.add(new InventoryReportRow(product.getId(), product.getName(),
                    product.getCategory() != null ? product.getCategory().getName() : null, product.isIngredient(),
                    product.unitName(QuantityUnit.BASE), t.purchased, t.sold, t.returned, t.wasted, t.adjustedIn,
                    t.adjustedOut, money(t.purchaseCost), money(t.salesCost), money(t.wasteCost), onHand,
                    inventory.getReorderLevel(), money(stockValues.get(product.getId()))));
        }
        rows.sort(Comparator.comparing(InventoryReportRow::ingredient)
                .thenComparing(InventoryReportRow::productName, String.CASE_INSENSITIVE_ORDER));

        return new InventoryReportResponse(period, from, to.minusDays(1),
                money(sum(totals, t -> t.purchaseCost)), expenses.size(),
                money(sum(totals, t -> t.salesCost)), saleCount,
                money(sum(totals, t -> t.wasteCost)), wasteCount, wasteByReason,
                money(sum(totals, t -> t.returnValue)), money(sum(totals, t -> t.adjustmentValue)),
                movementCount, money(stockValues.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)),
                lowStock, outOfStock, rows);
    }

    // Movements recorded before reasons were kept: stock in was a purchase, stock out a manual cut or a sale.
    private static StockMovementReason reasonOf(StockMovementReason reason, StockMovementType type) {
        if (reason != null) {
            return reason;
        }
        return type == StockMovementType.STOCK_IN ? StockMovementReason.PURCHASE : StockMovementReason.ADJUSTMENT;
    }

    @Override
    public byte[] exportSummary(ReportPeriod period, LocalDate date) {
        InventoryReportResponse report = summary(period, date);
        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle bold = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            bold.setFont(font);
            CellStyle money = workbook.createCellStyle();
            money.setDataFormat(workbook.createDataFormat().getFormat("$#,##0.00"));

            Sheet summary = workbook.createSheet("Summary");
            Object[][] lines = {
                    {"Period", period.name() + " " + report.from() + " to " + report.to()},
                    {"Purchases", report.purchaseCost()},
                    {"Purchase records", report.purchaseCount()},
                    {"Cost of goods sold", report.salesCost()},
                    {"Waste", report.wasteCost()},
                    {"Waste records", report.wasteCount()},
                    {"Returned to stock", report.returnValue()},
                    {"Stock count adjustments", report.adjustmentValue()},
                    {"Stock movements", report.movementCount()},
                    {"Stock value now", report.stockValue()},
                    {"Low stock products", report.lowStockCount()},
                    {"Out of stock products", report.outOfStockCount()}};
            for (int i = 0; i < lines.length; i++) {
                Row row = summary.createRow(i);
                Cell label = row.createCell(0);
                label.setCellValue((String) lines[i][0]);
                label.setCellStyle(bold);
                write(row.createCell(1), lines[i][1], money);
            }
            int wasteRow = lines.length + 1;
            for (Map.Entry<WasteReason, BigDecimal> entry : report.wasteCostByReason().entrySet()) {
                Row row = summary.createRow(wasteRow++);
                row.createCell(0).setCellValue("Waste: " + entry.getKey().name());
                write(row.createCell(1), entry.getValue(), money);
            }
            summary.autoSizeColumn(0);
            summary.autoSizeColumn(1);

            Sheet products = workbook.createSheet("Products");
            String[] headers = {"Product", "Category", "Ingredient", "Unit", "Purchased", "Sold", "Returned", "Wasted",
                    "Adjusted In", "Adjusted Out", "Purchase Cost", "Cost of Sales", "Waste Cost", "On Hand Now",
                    "Reorder Level", "Stock Value Now"};
            Row header = products.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(bold);
            }
            int rowIndex = 1;
            for (InventoryReportRow line : report.products()) {
                Row row = products.createRow(rowIndex++);
                Object[] values = {line.productName(), line.categoryName(), line.ingredient() ? "Yes" : "No", line.unit(),
                        line.purchased(), line.sold(), line.returned(), line.wasted(), line.adjustedIn(),
                        line.adjustedOut()};
                for (int i = 0; i < values.length; i++) {
                    write(row.createCell(i), values[i], null);
                }
                write(row.createCell(10), line.purchaseCost(), money);
                write(row.createCell(11), line.salesCost(), money);
                write(row.createCell(12), line.wasteCost(), money);
                write(row.createCell(13), line.onHand(), null);
                write(row.createCell(14), line.reorderLevel(), null);
                write(row.createCell(15), line.stockValue(), money);
            }
            for (int i = 0; i < headers.length; i++) {
                products.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new InvalidOperationException("Unable to generate inventory report: " + e.getMessage());
        }
    }

    private static void write(Cell cell, Object value, CellStyle moneyStyle) {
        if (value instanceof BigDecimal number) {
            cell.setCellValue(number.doubleValue());
            if (moneyStyle != null) {
                cell.setCellStyle(moneyStyle);
            }
        } else if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else if (value != null) {
            cell.setCellValue(value.toString());
        }
    }

    private static BigDecimal sum(Map<UUID, Totals> totals, Function<Totals, BigDecimal> value) {
        return totals.values().stream().map(value).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static BigDecimal money(BigDecimal value) {
        return orZero(value).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private static final class Totals {
        private BigDecimal purchased = BigDecimal.ZERO;
        private BigDecimal sold = BigDecimal.ZERO;
        private BigDecimal returned = BigDecimal.ZERO;
        private BigDecimal wasted = BigDecimal.ZERO;
        private BigDecimal adjustedIn = BigDecimal.ZERO;
        private BigDecimal adjustedOut = BigDecimal.ZERO;
        private BigDecimal purchaseCost = BigDecimal.ZERO;
        private BigDecimal salesCost = BigDecimal.ZERO;
        private BigDecimal wasteCost = BigDecimal.ZERO;
        private BigDecimal returnValue = BigDecimal.ZERO;
        private BigDecimal adjustmentValue = BigDecimal.ZERO;
    }
}
