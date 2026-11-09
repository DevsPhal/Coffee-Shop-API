package org.group1.coffeeshopapi.inventory.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.enums.QuantityUnit;
import org.group1.coffeeshopapi.common.enums.ReportPeriod;
import org.group1.coffeeshopapi.common.enums.StockMovementReason;
import org.group1.coffeeshopapi.common.enums.StockMovementType;
import org.group1.coffeeshopapi.common.enums.WasteReason;
import org.group1.coffeeshopapi.common.excel.BrandedSheet;
import org.group1.coffeeshopapi.common.excel.BrandedWorkbook;
import org.group1.coffeeshopapi.common.excel.ExcelText;
import org.group1.coffeeshopapi.common.excel.KeyValue;
import org.group1.coffeeshopapi.common.excel.TableColumn;
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

    private static final List<TableColumn> PRODUCT_COLUMNS = List.of(
            TableColumn.text("Product"), TableColumn.text("Category"), TableColumn.text("Ingredient"),
            TableColumn.text("Unit"), TableColumn.quantity("Purchased"), TableColumn.quantity("Sold"),
            TableColumn.quantity("Returned"), TableColumn.quantity("Wasted"), TableColumn.quantity("Adjusted In"),
            TableColumn.quantity("Adjusted Out"), TableColumn.money("Purchase Cost"), TableColumn.money("Cost of Sales"),
            TableColumn.money("Waste Cost"), TableColumn.quantity("On Hand Now"), TableColumn.quantity("Reorder Level"),
            TableColumn.money("Stock Value Now"));

    @Override
    public byte[] exportSummary(ReportPeriod period, LocalDate date) {
        InventoryReportResponse report = summary(period, date);
        String periodText = "Period: " + ExcelText.period(period, report.from(), report.to());
        try (BrandedWorkbook book = new BrandedWorkbook()) {
            BrandedSheet summary = book.sheet("Summary", "Inventory Report", List.of(periodText,
                    "Products: " + report.products().size() + " · Low stock: " + report.lowStockCount()
                            + " · Out of stock: " + report.outOfStockCount()));
            summary.section("Stock movements and value");
            summary.keyValues(List.of(
                    KeyValue.text("Period", period.name() + " " + report.from() + " to " + report.to()),
                    KeyValue.money("Purchases", report.purchaseCost()),
                    KeyValue.count("Purchase records", report.purchaseCount()),
                    KeyValue.money("Cost of goods sold", report.salesCost()),
                    KeyValue.money("Waste", report.wasteCost()),
                    KeyValue.count("Waste records", report.wasteCount()),
                    KeyValue.money("Returned to stock", report.returnValue()),
                    KeyValue.money("Stock count adjustments", report.adjustmentValue()),
                    KeyValue.count("Stock movements", report.movementCount()),
                    KeyValue.money("Stock value now", report.stockValue()),
                    KeyValue.count("Low stock products", report.lowStockCount()),
                    KeyValue.count("Out of stock products", report.outOfStockCount())));
            if (!report.wasteCostByReason().isEmpty()) {
                summary.blank();
                summary.section("Waste by reason");
                List<KeyValue> waste = new ArrayList<>();
                for (Map.Entry<WasteReason, BigDecimal> entry : report.wasteCostByReason().entrySet()) {
                    waste.add(KeyValue.money(ExcelText.readable(entry.getKey()), entry.getValue()));
                }
                summary.keyValues(waste);
            }

            BrandedSheet products = book.sheet("Products", "Inventory by Product", List.of(periodText,
                    "Quantities are in each product's own unit; money is in US dollars."));
            List<Object[]> rows = new ArrayList<>();
            for (InventoryReportRow line : report.products()) {
                rows.add(new Object[] {line.productName(), line.categoryName(), line.ingredient() ? "Yes" : "No",
                        line.unit(), line.purchased(), line.sold(), line.returned(), line.wasted(), line.adjustedIn(),
                        line.adjustedOut(), line.purchaseCost(), line.salesCost(), line.wasteCost(), line.onHand(),
                        line.reorderLevel(), line.stockValue()});
            }
            // Quantities are in different units per product, so only the money columns are totalled.
            products.table(PRODUCT_COLUMNS, rows, new Object[] {"Total", null, null, null, null, null, null, null,
                    null, null, sumMoney(report.products(), InventoryReportRow::purchaseCost),
                    sumMoney(report.products(), InventoryReportRow::salesCost),
                    sumMoney(report.products(), InventoryReportRow::wasteCost), null, null,
                    sumMoney(report.products(), InventoryReportRow::stockValue)});
            return book.toBytes();
        } catch (IOException e) {
            throw new InvalidOperationException("Unable to generate inventory report: " + e.getMessage());
        }
    }

    private static BigDecimal sumMoney(List<InventoryReportRow> rows, Function<InventoryReportRow, BigDecimal> value) {
        return rows.stream().map(value).map(InventoryReportServiceImpl::orZero).reduce(BigDecimal.ZERO, BigDecimal::add);
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
