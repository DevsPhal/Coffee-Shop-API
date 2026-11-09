package org.group1.coffeeshopapi.common.excel;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.HashMap;
import java.util.Map;

/**
 * The one look of every 590st Cafe Shop workbook: the brand raspberry for headers and titles, warm greys for
 * text and borders, Calibri throughout. Styles are created once per workbook and shared by every cell.
 */
public final class ExcelStyles {

    static final String FONT = "Calibri";

    // The customer app's brand colour and the neutrals that go with it.
    private static final XSSFColor BRAND = rgb(0xA1, 0x25, 0x5B);
    private static final XSSFColor BRAND_TINT = rgb(0xFB, 0xE7, 0xEF);
    private static final XSSFColor INK = rgb(0x1C, 0x14, 0x18);
    private static final XSSFColor MUTED = rgb(0x6D, 0x5F, 0x67);
    private static final XSSFColor BORDER = rgb(0xD9, 0xD2, 0xD6);
    private static final XSSFColor ZEBRA = rgb(0xFA, 0xF7, 0xF8);
    private static final XSSFColor WHITE = rgb(0xFF, 0xFF, 0xFF);
    private static final XSSFColor OPTIONAL_FILL = rgb(0xEF, 0xE8, 0xEC);
    private static final XSSFColor REFERENCE_FILL = rgb(0xE3, 0xEA, 0xF4);
    private static final XSSFColor REFERENCE_INK = rgb(0x2B, 0x3A, 0x55);

    static final String MONEY_FORMAT = "$#,##0.00;-$#,##0.00";
    static final String KHR_FORMAT = "#,##0 \"KHR\"";
    static final String INTEGER_FORMAT = "#,##0";
    static final String DECIMAL_FORMAT = "#,##0.0##";

    private final XSSFWorkbook workbook;
    private final Map<String, XSSFCellStyle> cache = new HashMap<>();
    private final Map<String, XSSFFont> fonts = new HashMap<>();

    ExcelStyles(XSSFWorkbook workbook) {
        this.workbook = workbook;
    }

    // ---- Banner ----------------------------------------------------------------------------------------

    XSSFCellStyle shopName() {
        return cache.computeIfAbsent("shopName", key -> plain(font(16, true, INK)));
    }

    XSSFCellStyle title() {
        return cache.computeIfAbsent("title", key -> plain(font(13, true, BRAND)));
    }

    XSSFCellStyle generated() {
        return cache.computeIfAbsent("generated", key -> plain(font(9, false, MUTED)));
    }

    XSSFCellStyle detail() {
        return cache.computeIfAbsent("detail", key -> plain(font(10, false, INK)));
    }

    /** The brand rule under the banner. */
    XSSFCellStyle rule() {
        return cache.computeIfAbsent("rule", key -> {
            XSSFCellStyle style = workbook.createCellStyle();
            style.setBorderBottom(BorderStyle.MEDIUM);
            style.setBottomBorderColor(BRAND);
            return style;
        });
    }

    /** A sub-heading inside a sheet, e.g. "Waste by reason". */
    public XSSFCellStyle section() {
        return cache.computeIfAbsent("section", key -> {
            XSSFCellStyle style = plain(font(11, true, BRAND));
            style.setBorderBottom(BorderStyle.THIN);
            style.setBottomBorderColor(BRAND);
            return style;
        });
    }

    // ---- Tables ----------------------------------------------------------------------------------------

    /** A table header cell, aligned like the column's values. */
    public XSSFCellStyle header(TableColumn.Kind kind) {
        return cache.computeIfAbsent("header:" + kind, key -> {
            XSSFCellStyle style = filled(font(10, true, WHITE), BRAND);
            style.setAlignment(kind == TableColumn.Kind.TEXT ? HorizontalAlignment.LEFT : HorizontalAlignment.RIGHT);
            style.setWrapText(true);
            outline(style, BRAND);
            return style;
        });
    }

    /** A table body cell. {@code striped} shades every other row; {@code total} is the bold totals row. */
    public XSSFCellStyle cell(TableColumn.Kind kind, boolean striped, boolean total) {
        return cache.computeIfAbsent("cell:" + kind + ":" + striped + ":" + total, key -> {
            XSSFCellStyle style = workbook.createCellStyle();
            style.setFont(font(10, total, INK));
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            style.setAlignment(kind == TableColumn.Kind.TEXT ? HorizontalAlignment.LEFT : HorizontalAlignment.RIGHT);
            String format = switch (kind) {
                case TEXT -> null;
                case INTEGER -> INTEGER_FORMAT;
                case QUANTITY -> DECIMAL_FORMAT;
                case MONEY -> MONEY_FORMAT;
                case KHR -> KHR_FORMAT;
            };
            if (format != null) {
                style.setDataFormat(workbook.createDataFormat().getFormat(format));
            }
            outline(style, BORDER);
            if (total) {
                style.setFillForegroundColor(BRAND_TINT);
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                style.setBorderTop(BorderStyle.MEDIUM);
                style.setTopBorderColor(BRAND);
            } else if (striped) {
                style.setFillForegroundColor(ZEBRA);
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }
            return style;
        });
    }

    /** The label of a summary line: bold, bordered like the table cells. */
    public XSSFCellStyle label(boolean striped) {
        String key = "label:" + striped;
        XSSFCellStyle cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        // Built outside computeIfAbsent: it starts from another cached style.
        XSSFCellStyle style = workbook.createCellStyle();
        style.cloneStyleFrom(cell(TableColumn.Kind.TEXT, striped, false));
        style.setFont(font(10, true, INK));
        cache.put(key, style);
        return style;
    }

    // ---- Import templates ------------------------------------------------------------------------------

    /** Header of a column the user must fill (marked *). */
    public XSSFCellStyle requiredHeader() {
        return cache.computeIfAbsent("requiredHeader", key -> templateHeader(WHITE, BRAND));
    }

    /** Header of a column the user may leave blank. */
    public XSSFCellStyle optionalHeader() {
        return cache.computeIfAbsent("optionalHeader", key -> templateHeader(INK, OPTIONAL_FILL));
    }

    /** Header of a column filled automatically or of a reference list. */
    public XSSFCellStyle referenceHeader() {
        return cache.computeIfAbsent("referenceHeader", key -> templateHeader(REFERENCE_INK, REFERENCE_FILL));
    }

    /** Cells kept as typed (SKUs with leading zeros, Khmer text). */
    public XSSFCellStyle textInput() {
        return cache.computeIfAbsent("textInput", key -> {
            XSSFCellStyle style = workbook.createCellStyle();
            style.setDataFormat(workbook.createDataFormat().getFormat("@"));
            return style;
        });
    }

    /** Long help text that wraps inside its column. */
    public XSSFCellStyle wrapped() {
        return cache.computeIfAbsent("wrapped", key -> {
            XSSFCellStyle style = plain(font(10, false, INK));
            style.setWrapText(true);
            style.setVerticalAlignment(VerticalAlignment.TOP);
            return style;
        });
    }

    /** Ordinary body text outside a table, e.g. the numbered steps of an instructions sheet. */
    public XSSFCellStyle body() {
        return detail();
    }

    // ---- Helpers ---------------------------------------------------------------------------------------

    private XSSFCellStyle templateHeader(XSSFColor fontColor, XSSFColor fill) {
        XSSFCellStyle style = filled(font(10, true, fontColor), fill);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(BORDER);
        style.setBorderRight(BorderStyle.THIN);
        style.setRightBorderColor(WHITE);
        return style;
    }

    private XSSFCellStyle plain(XSSFFont font) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private XSSFCellStyle filled(XSSFFont font, XSSFColor fill) {
        XSSFCellStyle style = plain(font);
        style.setFillForegroundColor(fill);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private static void outline(XSSFCellStyle style, XSSFColor color) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setTopBorderColor(color);
        style.setBottomBorderColor(color);
        style.setLeftBorderColor(color);
        style.setRightBorderColor(color);
    }

    private XSSFFont font(int points, boolean bold, XSSFColor color) {
        return fonts.computeIfAbsent(points + ":" + bold + ":" + color.getARGBHex(), key -> {
            XSSFFont font = workbook.createFont();
            font.setFontName(FONT);
            font.setFontHeightInPoints((short) points);
            font.setBold(bold);
            font.setColor(color);
            return font;
        });
    }

    private static XSSFColor rgb(int red, int green, int blue) {
        return new XSSFColor(new byte[] {(byte) red, (byte) green, (byte) blue}, null);
    }
}
