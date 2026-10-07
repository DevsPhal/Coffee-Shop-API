package org.group1.coffeeshopapi.common.excel;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public final class ExcelTemplateWriter implements AutoCloseable {

    private static final int TEMPLATE_ROWS = ExcelSheetReader.MAX_DATA_ROWS;

    private final XSSFWorkbook workbook = new XSSFWorkbook();
    private final CellStyle requiredHeader;
    private final CellStyle optionalHeader;
    private final CellStyle referenceHeader;
    private final CellStyle titleStyle;
    private final CellStyle wrapStyle;
    private final CellStyle textStyle;

    public ExcelTemplateWriter() {
        requiredHeader = headerStyle(IndexedColors.DARK_RED, IndexedColors.WHITE);
        optionalHeader = headerStyle(IndexedColors.GREY_25_PERCENT, IndexedColors.BLACK);
        referenceHeader = headerStyle(IndexedColors.LIGHT_CORNFLOWER_BLUE, IndexedColors.BLACK);

        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle = workbook.createCellStyle();
        titleStyle.setFont(titleFont);

        wrapStyle = workbook.createCellStyle();
        wrapStyle.setWrapText(true);
        wrapStyle.setVerticalAlignment(VerticalAlignment.TOP);

        textStyle = workbook.createCellStyle();
        textStyle.setDataFormat(workbook.createDataFormat().getFormat("@"));
    }

    public Workbook workbook() {
        return workbook;
    }

    public <C extends ExcelColumn> Sheet dataSheet(String name, List<C> columns) {
        Sheet sheet = workbook.createSheet(name);
        Row header = sheet.createRow(0);
        header.setHeightInPoints(22);
        CreationHelper helper = workbook.getCreationHelper();
        Drawing<?> drawing = sheet.createDrawingPatriarch();
        for (int i = 0; i < columns.size(); i++) {
            C column = columns.get(i);
            Cell cell = header.createCell(i);
            cell.setCellValue(column.headerLabel());
            cell.setCellStyle(column.input() ? (column.required() ? requiredHeader : optionalHeader) : referenceHeader);

            ClientAnchor anchor = helper.createClientAnchor();
            anchor.setCol1(i);
            anchor.setCol2(i + 4);
            anchor.setRow1(0);
            anchor.setRow2(6);
            Comment comment = drawing.createCellComment(anchor);
            comment.setString(helper.createRichTextString(column.description()
                    + "\nFormat: " + column.format() + "\nExample: " + column.example()));
            cell.setCellComment(comment);

            sheet.setColumnWidth(i, column.width() * 256);
        }
        sheet.createFreezePane(0, 1);
        return sheet;
    }

    public void textColumn(Sheet sheet, int columnIndex) {
        sheet.setDefaultColumnStyle(columnIndex, textStyle);
    }

    public void listValidation(Sheet sheet, int columnIndex, String[] values) {
        if (values.length == 0) {
            return;
        }
        DataValidationHelper helper = sheet.getDataValidationHelper();
        addValidation(sheet, helper, helper.createExplicitListConstraint(values), columnIndex,
                "Choose a value from the list: " + String.join(", ", values));
    }

    public void rangeValidation(Sheet sheet, int columnIndex, String rangeFormula, String hint) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        addValidation(sheet, helper, helper.createFormulaListConstraint(rangeFormula), columnIndex, hint);
    }

    public void decimalValidation(Sheet sheet, int columnIndex, String minimum, boolean inclusive) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        int operator = inclusive
                ? DataValidationConstraint.OperatorType.GREATER_OR_EQUAL
                : DataValidationConstraint.OperatorType.GREATER_THAN;
        addValidation(sheet, helper, helper.createDecimalConstraint(operator, minimum, null), columnIndex,
                "Enter a number " + (inclusive ? ">= " : "> ") + minimum);
    }

    public void formulaColumn(Sheet sheet, int columnIndex, String formulaWithRowToken) {
        for (int rowIndex = 1; rowIndex <= TEMPLATE_ROWS; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                row = sheet.createRow(rowIndex);
            }
            row.createCell(columnIndex).setCellFormula(formulaWithRowToken.replace("{row}", String.valueOf(rowIndex + 1)));
        }
    }

    public Sheet referenceSheet(String name, String[] headers, List<Object[]> rows) {
        Sheet sheet = workbook.createSheet(name);
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(referenceHeader);
        }
        for (int r = 0; r < rows.size(); r++) {
            Row row = sheet.createRow(r + 1);
            Object[] values = rows.get(r);
            for (int c = 0; c < values.length; c++) {
                Object value = values[c];
                Cell cell = row.createCell(c);
                if (value instanceof Number number) {
                    cell.setCellValue(number.doubleValue());
                } else if (value != null) {
                    cell.setCellValue(value.toString());
                }
            }
        }
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
        sheet.createFreezePane(0, 1);
        return sheet;
    }

    public <C extends ExcelColumn> Sheet guideSheet(String title, List<String> steps, List<C> columns,
            List<String[]> exampleRows) {
        Sheet sheet = workbook.createSheet("Instructions");
        int rowIndex = 0;

        Cell titleCell = sheet.createRow(rowIndex++).createCell(0);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(titleStyle);
        rowIndex++;

        for (int i = 0; i < steps.size(); i++) {
            Cell cell = sheet.createRow(rowIndex++).createCell(0);
            cell.setCellValue((i + 1) + ". " + steps.get(i));
        }
        rowIndex++;

        String[] headers = {"Column", "Required", "Format / allowed values", "What it means", "Example"};
        Row headerRow = sheet.createRow(rowIndex++);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(optionalHeader);
        }
        for (C column : columns) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(column.headerLabel());
            row.createCell(1).setCellValue(column.input() ? (column.required() ? "Yes" : "No") : "Auto (ignored)");
            writeWrapped(row, 2, column.format());
            writeWrapped(row, 3, column.description());
            row.createCell(4).setCellValue(column.example());
        }

        if (!exampleRows.isEmpty()) {
            rowIndex++;
            Cell exampleTitle = sheet.createRow(rowIndex++).createCell(0);
            exampleTitle.setCellValue("Example rows (for reference only — type your own data on the first sheet)");
            exampleTitle.setCellStyle(titleStyle);
            Row exampleHeader = sheet.createRow(rowIndex++);
            for (int i = 0; i < columns.size(); i++) {
                Cell cell = exampleHeader.createCell(i);
                cell.setCellValue(columns.get(i).headerLabel());
                cell.setCellStyle(optionalHeader);
            }
            for (String[] values : exampleRows) {
                Row row = sheet.createRow(rowIndex++);
                for (int i = 0; i < values.length; i++) {
                    row.createCell(i).setCellValue(values[i]);
                }
            }
        }

        sheet.setColumnWidth(0, 22 * 256);
        sheet.setColumnWidth(1, 16 * 256);
        sheet.setColumnWidth(2, 45 * 256);
        sheet.setColumnWidth(3, 60 * 256);
        sheet.setColumnWidth(4, 26 * 256);
        for (int i = 5; i < columns.size(); i++) {
            sheet.setColumnWidth(i, 18 * 256);
        }
        return sheet;
    }

    public byte[] toBytes() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.setActiveSheet(0);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new InvalidOperationException("Unable to generate Excel template: " + e.getMessage());
        }
    }

    @Override
    public void close() throws IOException {
        workbook.close();
    }

    private void addValidation(Sheet sheet, DataValidationHelper helper, DataValidationConstraint constraint,
            int columnIndex, String hint) {
        DataValidation validation = helper.createValidation(constraint,
                new CellRangeAddressList(1, TEMPLATE_ROWS, columnIndex, columnIndex));
        String message = hint.length() > 250 ? hint.substring(0, 247) + "..." : hint;
        validation.setShowErrorBox(true);
        validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
        validation.createErrorBox("Invalid value", message);
        validation.setShowPromptBox(true);
        validation.createPromptBox("Hint", message);
        if (constraint.getValidationType() == DataValidationConstraint.ValidationType.LIST) {
            validation.setSuppressDropDownArrow(true);
        }
        sheet.addValidationData(validation);
    }

    private void writeWrapped(Row row, int columnIndex, String value) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value);
        cell.setCellStyle(wrapStyle);
    }

    private CellStyle headerStyle(IndexedColors background, IndexedColors fontColor) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(fontColor.getIndex());
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(background.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }
}
