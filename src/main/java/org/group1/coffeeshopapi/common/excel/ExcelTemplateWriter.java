package org.group1.coffeeshopapi.common.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An import template (products, stock-in) with the same branding as every other Excel file: each sheet opens
 * with the logo and shop name. The data sheet's header row sits under that banner; {@link ExcelSheetReader}
 * finds it by its column names, so templates downloaded before the banner existed still import.
 */
public final class ExcelTemplateWriter implements AutoCloseable {

    private static final int TEMPLATE_ROWS = ExcelSheetReader.MAX_DATA_ROWS;
    private static final List<String> DATA_DETAILS = List.of(
            "Fill one row per line, starting right under the header row. Keep the header row as it is.",
            "Red headers (*) are required, grey are optional, blue fill in by themselves. Hover a header for help.");
    private static final List<String> REFERENCE_DETAILS = List.of(
            "For reference while you fill in the template. Changes on this sheet are not imported.");
    private static final List<String> GUIDE_DETAILS = List.of(
            "How to fill in this file, step by step and column by column.");

    private final BrandedWorkbook book = new BrandedWorkbook();
    private final ExcelStyles styles = book.styles();
    private final String title;
    private final Map<Sheet, Integer> headerRows = new HashMap<>();

    /** @param title the template's name, shown on every sheet, e.g. "Product Import Template". */
    public ExcelTemplateWriter(String title) {
        this.title = title;
    }

    public Workbook workbook() {
        return book.workbook();
    }

    public <C extends ExcelColumn> Sheet dataSheet(String name, List<C> columns) {
        BrandedSheet branded = book.sheet(name, title, DATA_DETAILS).gridlines().printWholeSheet();
        Sheet sheet = branded.sheet();
        Row header = branded.headerRow();
        CreationHelper helper = book.workbook().getCreationHelper();
        Drawing<?> drawing = sheet.createDrawingPatriarch();
        for (int i = 0; i < columns.size(); i++) {
            C column = columns.get(i);
            Cell cell = header.createCell(i);
            cell.setCellValue(column.headerLabel());
            cell.setCellStyle(column.input()
                    ? (column.required() ? styles.requiredHeader() : styles.optionalHeader())
                    : styles.referenceHeader());

            ClientAnchor anchor = helper.createClientAnchor();
            anchor.setCol1(i);
            anchor.setCol2(i + 4);
            anchor.setRow1(header.getRowNum());
            anchor.setRow2(header.getRowNum() + 6);
            Comment comment = drawing.createCellComment(anchor);
            comment.setString(helper.createRichTextString(column.description()
                    + "\nFormat: " + column.format() + "\nExample: " + column.example()));
            cell.setCellComment(comment);

            branded.width(i, column.width());
        }
        headerRows.put(sheet, header.getRowNum());
        return sheet;
    }

    public void textColumn(Sheet sheet, int columnIndex) {
        sheet.setDefaultColumnStyle(columnIndex, styles.textInput());
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

    public void wholeNumberValidation(Sheet sheet, int columnIndex, String minimum) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        addValidation(sheet, helper, helper.createIntegerConstraint(
                        DataValidationConstraint.OperatorType.GREATER_OR_EQUAL, minimum, null), columnIndex,
                "Enter a whole number >= " + minimum);
    }

    /** Fills a column with a formula on every template row; {row} becomes that row's Excel row number. */
    public void formulaColumn(Sheet sheet, int columnIndex, String formulaWithRowToken) {
        int header = headerRow(sheet);
        for (int offset = 1; offset <= TEMPLATE_ROWS; offset++) {
            int rowIndex = header + offset;
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                row = sheet.createRow(rowIndex);
            }
            row.createCell(columnIndex).setCellFormula(
                    formulaWithRowToken.replace("{row}", String.valueOf(rowIndex + 1)));
        }
    }

    /**
     * The cells of a reference sheet's first column holding {@code count} entries, e.g. "'Categories'!$A$9:$A$14",
     * for a dropdown. Usable before the reference sheet is written: its banner is always the same height.
     */
    public static String referenceRange(String sheetName, int count) {
        int firstExcelRow = BrandedSheet.firstContentRow(REFERENCE_DETAILS.size()) + 2;
        return "'" + sheetName + "'!$A$" + firstExcelRow + ":$A$" + (firstExcelRow + Math.max(count, 1) - 1);
    }

    public Sheet referenceSheet(String name, String[] headers, List<Object[]> rows) {
        BrandedSheet branded = book.sheet(name, name + " (reference)", REFERENCE_DETAILS);
        List<TableColumn> columns = java.util.Arrays.stream(headers).map(TableColumn::text).toList();
        branded.table(columns, rows, null);
        return branded.sheet();
    }

    public <C extends ExcelColumn> Sheet guideSheet(String guideTitle, List<String> steps, List<C> columns,
            List<String[]> exampleRows) {
        BrandedSheet branded = book.sheet("Instructions", guideTitle, GUIDE_DETAILS).noFreeze();

        branded.section("Steps");
        for (int i = 0; i < steps.size(); i++) {
            branded.text(branded.row(), 0, (i + 1) + ". " + steps.get(i), styles.body());
        }
        branded.blank();

        branded.section("Columns");
        String[] headers = {"Column", "Required", "Format / allowed values", "What it means", "Example"};
        Row headerRow = branded.row();
        for (int i = 0; i < headers.length; i++) {
            branded.text(headerRow, i, headers[i], styles.optionalHeader());
        }
        for (C column : columns) {
            Row row = branded.row();
            branded.text(row, 0, column.headerLabel(), styles.body());
            branded.text(row, 1, column.input() ? (column.required() ? "Yes" : "No") : "Auto (ignored)", styles.body());
            branded.text(row, 2, column.format(), styles.wrapped());
            branded.text(row, 3, column.description(), styles.wrapped());
            branded.text(row, 4, column.example(), styles.body());
        }

        if (!exampleRows.isEmpty()) {
            branded.blank();
            branded.section("Example rows (for reference only — type your own data on the first sheet)");
            Row exampleHeader = branded.row();
            for (int i = 0; i < columns.size(); i++) {
                branded.text(exampleHeader, i, columns.get(i).headerLabel(), styles.optionalHeader());
            }
            for (String[] values : exampleRows) {
                Row row = branded.row();
                for (int i = 0; i < values.length; i++) {
                    branded.text(row, i, values[i], styles.body());
                }
            }
        }

        branded.width(0, 22).width(1, 16).width(2, 45).width(3, 60).width(4, 26);
        for (int i = 5; i < columns.size(); i++) {
            branded.width(i, 18);
        }
        return branded.sheet();
    }

    public byte[] toBytes() {
        return book.toBytes();
    }

    @Override
    public void close() throws IOException {
        book.close();
    }

    private int headerRow(Sheet sheet) {
        Integer row = headerRows.get(sheet);
        if (row == null) {
            throw new IllegalArgumentException("Not a data sheet of this template: " + sheet.getSheetName());
        }
        return row;
    }

    private void addValidation(Sheet sheet, DataValidationHelper helper, DataValidationConstraint constraint,
            int columnIndex, String hint) {
        int header = headerRow(sheet);
        DataValidation validation = helper.createValidation(constraint,
                new CellRangeAddressList(header + 1, header + TEMPLATE_ROWS, columnIndex, columnIndex));
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
}
