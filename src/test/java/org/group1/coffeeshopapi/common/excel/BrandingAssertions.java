package org.group1.coffeeshopapi.common.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.util.Units;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFShape;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Checks shared by every Excel export test: the 590st Cafe Shop branding is on every sheet, on screen and on paper. */
public final class BrandingAssertions {

    private BrandingAssertions() {
    }

    public static XSSFWorkbook open(byte[] xlsx) throws IOException {
        return new XSSFWorkbook(new ByteArrayInputStream(xlsx));
    }

    /** Every sheet shows the logo top-left, the shop name, a title and when it was made, and prints them. */
    public static void assertEverySheetBranded(XSSFWorkbook workbook) {
        assertThat(workbook.getNumberOfSheets()).isPositive();
        assertThat(workbook.getProperties().getCoreProperties().getCreator()).isEqualTo(BrandedWorkbook.SHOP_NAME);
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            assertBranded(workbook.getSheetAt(i));
        }
    }

    public static void assertBranded(XSSFSheet sheet) {
        String name = sheet.getSheetName();

        XSSFDrawing drawing = sheet.getDrawingPatriarch();
        assertThat(drawing).as(name + " has a drawing").isNotNull();
        List<XSSFPicture> pictures = drawing.getShapes().stream()
                .filter(XSSFPicture.class::isInstance).map(XSSFPicture.class::cast).toList();
        assertThat(pictures).as(name + " has the logo").hasSize(1);
        XSSFPicture logo = pictures.get(0);
        XSSFClientAnchor anchor = logo.getClientAnchor();
        assertThat(anchor.getRow1()).as(name + " logo row").isZero();
        assertThat(anchor.getCol1()).as(name + " logo column").isZero();
        assertThat(logo.getPictureData().getData()).as(name + " logo is a PNG").startsWith(0x89, 'P', 'N', 'G');
        // Drawn at a readable size inside the tall first row: about 58 px high.
        assertThat(anchor.getRow2()).as(name + " logo stays in the first row").isZero();
        assertThat((anchor.getDy2() - anchor.getDy1()) / (double) Units.EMU_PER_PIXEL)
                .as(name + " logo height in px").isCloseTo(58, within(1.0));

        assertThat(text(sheet, BrandedSheet.SHOP_NAME_ROW)).as(name + " shop name").isEqualTo("590st Cafe Shop");
        assertThat(text(sheet, BrandedSheet.TITLE_ROW)).as(name + " title").isNotBlank();
        assertThat(text(sheet, BrandedSheet.GENERATED_ROW)).as(name + " generated time")
                .matches("Generated \\d{1,2} [A-Z][a-z]{2} \\d{4}, \\d{2}:\\d{2} \\(Cambodia time\\)");

        // Size code first: "&\"Calibri,Bold\"&10590st..." would print the name in a 10,590 pt font.
        assertThat(sheet.getHeader().getLeft()).as(name + " page header")
                .isEqualTo("&10&\"Calibri,Bold\"590st Cafe Shop");
        assertThat(sheet.getHeader().getRight()).as(name + " page header title").contains(text(sheet, BrandedSheet.TITLE_ROW));
        assertThat(sheet.getFooter().getRight()).as(name + " page numbers").contains("Page &P of &N");
        assertThat(sheet.getFitToPage()).as(name + " fits the page width").isTrue();
        assertThat(sheet.getPrintSetup().getFitWidth()).isEqualTo((short) 1);
        assertThat(sheet.getPrintSetup().getFitHeight()).isZero();
        assertThat(sheet.getPrintSetup().getPaperSize()).isEqualTo(PrintSetup.A4_PAPERSIZE);
    }

    /** The text of column A on a row, or "" when the row or cell is missing. */
    public static String text(Sheet sheet, int rowIndex) {
        Row row = sheet.getRow(rowIndex);
        if (row == null || row.getCell(0) == null || row.getCell(0).getCellType() != CellType.STRING) {
            return "";
        }
        return row.getCell(0).getStringCellValue();
    }

    /** The first row whose column A reads {@code label}. */
    public static Row rowStartingWith(Sheet sheet, String label) {
        return findRow(sheet, label).orElseThrow(
                () -> new AssertionError("No row starting with '" + label + "' on " + sheet.getSheetName()));
    }

    public static Optional<Row> findRow(Sheet sheet, String label) {
        for (Row row : sheet) {
            Cell first = row.getCell(0);
            if (first != null && first.getCellType() == CellType.STRING && first.getStringCellValue().equals(label)) {
                return Optional.of(row);
            }
        }
        return Optional.empty();
    }

    /** Whether the drawing holds any picture (used to show a sheet without branding fails the check). */
    public static boolean hasPicture(XSSFSheet sheet) {
        XSSFDrawing drawing = sheet.getDrawingPatriarch();
        if (drawing == null) {
            return false;
        }
        for (XSSFShape shape : drawing.getShapes()) {
            if (shape instanceof XSSFPicture) {
                return true;
            }
        }
        return false;
    }
}
