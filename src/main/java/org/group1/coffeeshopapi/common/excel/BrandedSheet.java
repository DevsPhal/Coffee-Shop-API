package org.group1.coffeeshopapi.common.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.PageMargin;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.Units;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFPrintSetup;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * One branded sheet. The constructor writes the banner (logo row, shop name, title, generated time, details,
 * brand rule); content methods append below it; {@link BrandedWorkbook#toBytes()} calls {@link #finish()}.
 *
 * <p>Banner rows: 0 logo, 1 shop name, 2 title, 3 generated time, then one row per detail, the rule and a
 * spacer. Content starts at {@link #nextRow()}.
 */
public final class BrandedSheet {

    public static final int SHOP_NAME_ROW = 1;
    public static final int TITLE_ROW = 2;
    public static final int GENERATED_ROW = 3;

    private static final float LOGO_ROW_POINTS = 50f;
    private static final double LOGO_HEIGHT_PX = 58;
    private static final int LOGO_INSET_PX = 4;
    private static final double MIN_WIDTH = 8;
    private static final double MAX_WIDTH = 60;
    /** Landscape once a table has this many columns. */
    private static final int LANDSCAPE_FROM_COLUMNS = 8;

    private final BrandedWorkbook book;
    private final ExcelStyles styles;
    private final XSSFSheet sheet;
    private final String title;
    private final List<Integer> bannerTextRows = new ArrayList<>();
    private final List<Integer> sectionRows = new ArrayList<>();
    private final Map<Integer, Double> contentWidths = new HashMap<>();
    private final Map<Integer, Double> fixedWidths = new HashMap<>();
    private final int ruleRow;
    private double bannerWidth;
    private int nextRow;
    private int lastColumn;
    private int headerRow = -1;
    private boolean landscape;
    private boolean gridlines;
    private boolean freezeHeader = true;
    private boolean printWholeSheet;
    private boolean finished;

    BrandedSheet(BrandedWorkbook book, XSSFSheet sheet, String title, List<String> details) {
        this.book = book;
        this.styles = book.styles();
        this.sheet = sheet;
        this.title = title;

        sheet.createRow(0).setHeightInPoints(LOGO_ROW_POINTS);
        bannerLine(SHOP_NAME_ROW, BrandedWorkbook.SHOP_NAME, styles.shopName(), 22f, 1.6);
        bannerLine(TITLE_ROW, title, styles.title(), 19f, 1.3);
        bannerLine(GENERATED_ROW, book.generatedText(), styles.generated(), 14f, 0.85);
        int row = GENERATED_ROW + 1;
        for (String detail : details) {
            bannerLine(row++, detail, styles.detail(), 15f, 0.95);
        }
        ruleRow = row++;
        sheet.createRow(ruleRow).setHeightInPoints(6f);
        sheet.createRow(row++).setHeightInPoints(8f);
        nextRow = row;
    }

    /** The first row under a banner with {@code detailLines} detail lines (logo, name, title, time, details, rule, spacer). */
    public static int firstContentRow(int detailLines) {
        return GENERATED_ROW + 1 + detailLines + 2;
    }

    // ---- Content ---------------------------------------------------------------------------------------

    /** A sub-heading row, e.g. "Waste by reason". */
    public BrandedSheet section(String heading) {
        Row row = sheet.createRow(nextRow++);
        row.setHeightInPoints(20f);
        text(row, 0, heading, styles.section(), 1.15);
        sectionRows.add(row.getRowNum());
        return this;
    }

    /** An empty row between blocks. */
    public BrandedSheet blank() {
        nextRow++;
        return this;
    }

    /** Labelled figures, one per row: the label in column A, the value in column B. */
    public BrandedSheet keyValues(List<KeyValue> lines) {
        boolean striped = false;
        for (KeyValue line : lines) {
            Row row = sheet.createRow(nextRow++);
            text(row, 0, line.label(), styles.label(striped), 1.05);
            value(row, 1, line.value(), line.kind(), striped, false);
            striped = !striped;
        }
        lastColumn = Math.max(lastColumn, 1);
        return this;
    }

    /**
     * A table: a brand-coloured header row, striped body rows and, when {@code totals} is given, a bold totals
     * row. Returns the header row index. The first table's header repeats on every printed page.
     */
    public int table(List<TableColumn> columns, List<Object[]> rows, Object[] totals) {
        int header = nextRow++;
        Row headerCells = sheet.createRow(header);
        headerCells.setHeightInPoints(22f);
        for (int i = 0; i < columns.size(); i++) {
            text(headerCells, i, columns.get(i).header(), styles.header(columns.get(i).kind()), 1.1);
        }
        boolean striped = false;
        for (Object[] values : rows) {
            Row row = sheet.createRow(nextRow++);
            for (int i = 0; i < columns.size(); i++) {
                value(row, i, i < values.length ? values[i] : null, columns.get(i).kind(), striped, false);
            }
            striped = !striped;
        }
        if (totals != null) {
            Row row = sheet.createRow(nextRow++);
            for (int i = 0; i < columns.size(); i++) {
                value(row, i, i < totals.length ? totals[i] : null, columns.get(i).kind(), false, true);
            }
        }
        if (headerRow < 0) {
            headerRow = header;
        }
        lastColumn = Math.max(lastColumn, columns.size() - 1);
        return header;
    }

    // ---- Building blocks for sheets with their own layout (import templates) ---------------------------

    /** Starts the header row of a data-entry table at the next row and returns it. */
    public Row headerRow() {
        Row row = sheet.createRow(nextRow++);
        row.setHeightInPoints(22f);
        headerRow = row.getRowNum();
        return row;
    }

    /** The next free row, created. */
    public Row row() {
        return sheet.createRow(nextRow++);
    }

    /** Writes text into a cell and widens its column to fit. */
    public Cell text(Row row, int column, String text, CellStyle style) {
        return text(row, column, text, style, 1.0);
    }

    /** Sets a column's width in characters; it is not widened to fit its content. */
    public BrandedSheet width(int column, double characters) {
        fixedWidths.put(column, characters);
        lastColumn = Math.max(lastColumn, column);
        return this;
    }

    /** Widest column the banner rule and merged banner lines span (columns past the content stay unused). */
    public BrandedSheet spanColumns(int columns) {
        lastColumn = Math.max(lastColumn, columns - 1);
        return this;
    }

    public BrandedSheet landscape() {
        landscape = true;
        return this;
    }

    /** Shows the grid (sheets people type into). */
    public BrandedSheet gridlines() {
        gridlines = true;
        return this;
    }

    public BrandedSheet noFreeze() {
        freezeHeader = false;
        return this;
    }

    /** Leaves the print area to Excel (template rows people fill in later). */
    public BrandedSheet printWholeSheet() {
        printWholeSheet = true;
        return this;
    }

    public XSSFSheet sheet() {
        return sheet;
    }

    public int nextRow() {
        return nextRow;
    }

    /** The header row of the first table, or -1. */
    public int headerRowIndex() {
        return headerRow;
    }

    public ExcelStyles styles() {
        return styles;
    }

    // ---- Layout ----------------------------------------------------------------------------------------

    void finish() {
        if (finished) {
            return;
        }
        finished = true;

        double[] widths = new double[lastColumn + 1];
        double total = 0;
        for (int column = 0; column <= lastColumn; column++) {
            Double fixed = fixedWidths.get(column);
            widths[column] = fixed != null ? fixed
                    : Math.min(MAX_WIDTH, Math.max(MIN_WIDTH, contentWidths.getOrDefault(column, MIN_WIDTH)));
            total += widths[column];
        }
        // The banner lines are merged across the table, so the table must be wide enough to hold them.
        if (bannerWidth > total) {
            widths[lastColumn] += bannerWidth - total;
        }
        for (int column = 0; column <= lastColumn; column++) {
            sheet.setColumnWidth(column, (int) Math.round(Math.min(255, widths[column]) * 256));
        }

        Row rule = sheet.getRow(ruleRow);
        for (int column = 0; column <= lastColumn; column++) {
            rule.createCell(column).setCellStyle(styles.rule());
        }
        // A section heading's underline runs the width of the table, not just under its first cell.
        for (int rowIndex : sectionRows) {
            Row row = sheet.getRow(rowIndex);
            for (int column = 1; column <= lastColumn; column++) {
                if (row.getCell(column) == null) {
                    row.createCell(column).setCellStyle(styles.section());
                }
            }
        }
        if (lastColumn > 0) {
            for (int row : bannerTextRows) {
                sheet.addMergedRegion(new CellRangeAddress(row, row, 0, lastColumn));
            }
        }

        placeLogo();
        printSetup();
        if (freezeHeader && headerRow >= 0) {
            sheet.createFreezePane(0, headerRow + 1);
        }
    }

    // The logo sits inside the tall first row at a fixed size, keeping its shape whatever the column widths:
    // its right edge is found by walking across the columns it covers.
    private void placeLogo() {
        int inset = Units.pixelToEMU(LOGO_INSET_PX);
        long height = Math.round(LOGO_HEIGHT_PX * Units.EMU_PER_PIXEL);
        long right = inset + Math.round(LOGO_HEIGHT_PX * book.logoAspectRatio() * Units.EMU_PER_PIXEL);
        int lastColumnCovered = 0;
        long columnWidth = Math.round(sheet.getColumnWidthInPixels(0) * Units.EMU_PER_PIXEL);
        while (right > columnWidth) {
            right -= columnWidth;
            lastColumnCovered++;
            columnWidth = Math.round(sheet.getColumnWidthInPixels(lastColumnCovered) * Units.EMU_PER_PIXEL);
        }
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(inset, inset, (int) right, (int) (inset + height),
                0, 0, lastColumnCovered, 0);
        anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_DONT_RESIZE);
        drawing.createPicture(anchor, book.logo());
    }

    private void printSetup() {
        XSSFPrintSetup print = sheet.getPrintSetup();
        print.setPaperSize(PrintSetup.A4_PAPERSIZE);
        print.setLandscape(landscape || lastColumn + 1 >= LANDSCAPE_FROM_COLUMNS);
        print.setFitWidth((short) 1);
        print.setFitHeight((short) 0);
        sheet.setFitToPage(true);
        sheet.setAutobreaks(true);
        sheet.setHorizontallyCenter(true);
        sheet.setMargin(PageMargin.LEFT, 0.4);
        sheet.setMargin(PageMargin.RIGHT, 0.4);
        sheet.setMargin(PageMargin.TOP, 0.75);
        sheet.setMargin(PageMargin.BOTTOM, 0.75);
        sheet.setMargin(PageMargin.HEADER, 0.3);
        sheet.setMargin(PageMargin.FOOTER, 0.3);
        sheet.setDisplayGridlines(gridlines);
        sheet.setPrintGridlines(false);
        if (headerRow >= 0) {
            sheet.setRepeatingRows(new CellRangeAddress(headerRow, headerRow, -1, -1));
        }
        // Every printed page carries the shop name and the report title; the logo heads the first page.
        // The size code comes before the font code: "&10590st" would read as a 10,590 pt font.
        sheet.getHeader().setLeft(headerText(10, "Bold", BrandedWorkbook.SHOP_NAME));
        sheet.getHeader().setRight(headerText(9, "Regular", title));
        sheet.getFooter().setLeft(headerText(8, "Regular", book.generatedText()));
        sheet.getFooter().setRight("&8&\"Calibri,Regular\"Page &P of &N");
        if (!printWholeSheet) {
            int index = sheet.getWorkbook().getSheetIndex(sheet);
            sheet.getWorkbook().setPrintArea(index, 0, lastColumn, 0, Math.max(sheet.getLastRowNum(), 0));
        }
    }

    // ---- Cells -----------------------------------------------------------------------------------------

    private void bannerLine(int rowIndex, String text, CellStyle style, float height, double scale) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(height);
        Cell cell = row.createCell(0);
        cell.setCellValue(text);
        cell.setCellStyle(style);
        bannerTextRows.add(rowIndex);
        bannerWidth = Math.max(bannerWidth, text.length() * scale + 2);
    }

    private Cell text(Row row, int column, String text, CellStyle style, double scale) {
        Cell cell = row.createCell(column);
        if (text != null) {
            cell.setCellValue(text);
            track(column, text, scale);
        }
        cell.setCellStyle(style);
        return cell;
    }

    private void value(Row row, int column, Object value, TableColumn.Kind kind, boolean striped, boolean total) {
        Cell cell = row.createCell(column);
        TableColumn.Kind shown = kind;
        if (value instanceof Number number) {
            BigDecimal decimal = number instanceof BigDecimal big ? big : new BigDecimal(number.toString());
            // Quantities (and numbers in a text column) show whole numbers without decimals.
            if (kind == TableColumn.Kind.QUANTITY || kind == TableColumn.Kind.TEXT) {
                shown = decimal.stripTrailingZeros().scale() <= 0 ? TableColumn.Kind.INTEGER : TableColumn.Kind.QUANTITY;
            }
            cell.setCellValue(decimal.doubleValue());
            track(column, formatted(decimal, shown), total ? 1.1 : 1.0);
        } else if (value != null) {
            cell.setCellValue(value.toString());
            shown = TableColumn.Kind.TEXT;
            track(column, value.toString(), total ? 1.1 : 1.0);
        }
        cell.setCellStyle(styles.cell(shown, striped, total));
    }

    private static String formatted(BigDecimal value, TableColumn.Kind kind) {
        return switch (kind) {
            case MONEY -> String.format(Locale.ENGLISH, "$%,.2f", value);
            case KHR -> String.format(Locale.ENGLISH, "%,.0f KHR", value);
            case INTEGER -> String.format(Locale.ENGLISH, "%,.0f", value);
            default -> value.stripTrailingZeros().toPlainString();
        };
    }

    private void track(int column, String text, double scale) {
        contentWidths.merge(column, text.length() * scale + 2.5, Math::max);
        lastColumn = Math.max(lastColumn, column);
    }

    // Size, then font, then the text with any literal "&" doubled (a single "&" starts an Excel header code).
    static String headerText(int points, String weight, String text) {
        return "&" + points + "&\"" + ExcelStyles.FONT + "," + weight + "\"" + text.replace("&", "&&");
    }
}
