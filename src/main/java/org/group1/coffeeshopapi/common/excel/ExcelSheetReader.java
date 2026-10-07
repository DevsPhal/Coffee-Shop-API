package org.group1.coffeeshopapi.common.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ExcelSheetReader<C extends Enum<C> & ExcelColumn> {

    public static final int MAX_DATA_ROWS = 1000;

    private final Sheet sheet;
    private final DataFormatter formatter = new DataFormatter();
    private final FormulaEvaluator evaluator;
    private final Map<C, Integer> indexes;
    private final List<C> inputColumns;

    private ExcelSheetReader(Sheet sheet, Map<C, Integer> indexes, List<C> inputColumns) {
        this.sheet = sheet;
        this.evaluator = sheet.getWorkbook().getCreationHelper().createFormulaEvaluator();
        this.indexes = indexes;
        this.inputColumns = inputColumns;
    }

    public static <C extends Enum<C> & ExcelColumn> ExcelSheetReader<C> open(
            Workbook workbook, String preferredSheet, Class<C> columnType) {
        Sheet sheet = workbook.getSheet(preferredSheet);
        if (sheet == null) {
            sheet = workbook.getSheetAt(0);
        }
        C[] columns = columnType.getEnumConstants();
        Map<C, Integer> indexes = resolveIndexes(sheet, columnType, columns);
        List<C> inputColumns = indexes.keySet().stream().filter(ExcelColumn::input).toList();

        List<String> missing = Arrays.stream(columns)
                .filter(column -> column.required() && !indexes.containsKey(column))
                .map(ExcelColumn::header)
                .toList();
        if (!missing.isEmpty()) {
            throw new InvalidOperationException("The sheet '" + sheet.getSheetName() + "' is missing required column(s): "
                    + String.join(", ", missing) + ". Download the import template and keep its header row.");
        }
        if (sheet.getLastRowNum() > MAX_DATA_ROWS) {
            throw new InvalidOperationException("An import file may contain at most " + MAX_DATA_ROWS
                    + " data rows — split the file and import it in parts");
        }
        return new ExcelSheetReader<>(sheet, indexes, inputColumns);
    }

    public int lastRowIndex() {
        return sheet.getLastRowNum();
    }

    public Row row(int rowIndex) {
        return sheet.getRow(rowIndex);
    }

    public boolean isBlank(Row row) {
        if (row == null) {
            return true;
        }
        for (C column : inputColumns) {
            if (!text(row, column).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public String text(Row row, C column) {
        Integer index = indexes.get(column);
        if (row == null || index == null) {
            return "";
        }
        Cell cell = row.getCell(index);
        if (cell == null) {
            return "";
        }
        return formatter.formatCellValue(cell, evaluator).trim();
    }

    public static BigDecimal parseDecimal(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(text.replace(",", "").replace("$", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static <E extends Enum<E>> E parseEnum(Class<E> type, String text) {
        try {
            return Enum.valueOf(type, text.trim().toUpperCase(Locale.ROOT).replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static String normalizeHeader(String raw) {
        return raw == null ? "" : raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static <C extends Enum<C> & ExcelColumn> Map<C, Integer> resolveIndexes(
            Sheet sheet, Class<C> columnType, C[] columns) {
        Map<C, Integer> byHeader = new EnumMap<>(columnType);
        Row header = sheet.getRow(0);
        if (header != null) {
            DataFormatter formatter = new DataFormatter();
            for (Cell cell : header) {
                String key = normalizeHeader(formatter.formatCellValue(cell));
                for (C column : columns) {
                    boolean matches = normalizeHeader(column.header()).equals(key) || column.aliases().contains(key);
                    if (matches && !byHeader.containsKey(column)) {
                        byHeader.put(column, cell.getColumnIndex());
                    }
                }
            }
        }
        if (!byHeader.isEmpty()) {
            return byHeader;
        }
        Map<C, Integer> legacy = new EnumMap<>(columnType);
        for (C column : columns) {
            if (column.legacyIndex() >= 0) {
                legacy.put(column, column.legacyIndex());
            }
        }
        return legacy;
    }
}
