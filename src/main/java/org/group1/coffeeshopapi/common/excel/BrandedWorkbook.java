package org.group1.coffeeshopapi.common.excel;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Every Excel file the system hands out, with the same 590st Cafe Shop branding. Each sheet opens with the
 * logo, the shop name, the report title, when it was generated and what it covers, then its data in styled
 * tables. Paper, orientation, margins, fit-to-width, the repeated header row and the page header and footer
 * are set on every sheet, so the branding prints as well as it shows.
 *
 * <pre>
 * try (BrandedWorkbook book = new BrandedWorkbook()) {
 *     book.sheet("Summary", "Sales report", List.of("Period: 1 Nov 2026 to 30 Nov 2026"))
 *         .keyValues(List.of(KeyValue.money("Money in", total)));
 *     return book.toBytes();
 * }
 * </pre>
 */
public final class BrandedWorkbook implements AutoCloseable {

    public static final String SHOP_NAME = "590st Cafe Shop";
    public static final ZoneId SHOP_ZONE = ZoneId.of("Asia/Phnom_Penh");
    static final String LOGO_RESOURCE = "/branding/590st-cafe-logo.png";
    private static final DateTimeFormatter GENERATED_FORMAT =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH);

    private final XSSFWorkbook workbook = new XSSFWorkbook();
    private final ExcelStyles styles = new ExcelStyles(workbook);
    private final List<BrandedSheet> sheets = new ArrayList<>();
    private final String generatedText;
    private int logoIndex = -1;
    private double logoAspectRatio;

    public BrandedWorkbook() {
        this(LocalDateTime.now(SHOP_ZONE));
    }

    public BrandedWorkbook(LocalDateTime generatedAt) {
        this.generatedText = "Generated " + GENERATED_FORMAT.format(generatedAt) + " (Cambodia time)";
        workbook.getProperties().getCoreProperties().setCreator(SHOP_NAME);
        workbook.getProperties().getExtendedProperties().getUnderlyingProperties().setCompany(SHOP_NAME);
    }

    /** A new sheet that starts with the branded banner; add its content below it. */
    public BrandedSheet sheet(String name, String title, List<String> details) {
        BrandedSheet sheet = new BrandedSheet(this, workbook.createSheet(name), title, details);
        sheets.add(sheet);
        if (sheets.size() == 1) {
            workbook.getProperties().getCoreProperties().setTitle(title);
        }
        return sheet;
    }

    public XSSFWorkbook workbook() {
        return workbook;
    }

    public ExcelStyles styles() {
        return styles;
    }

    String generatedText() {
        return generatedText;
    }

    int logo() {
        if (logoIndex < 0) {
            try (InputStream in = BrandedWorkbook.class.getResourceAsStream(LOGO_RESOURCE)) {
                if (in == null) {
                    throw new IllegalStateException("Missing logo resource " + LOGO_RESOURCE);
                }
                byte[] png = in.readAllBytes();
                // A PNG's size sits in its header: width at byte 16, height at byte 20.
                logoAspectRatio = (double) ByteBuffer.wrap(png, 16, 4).getInt() / ByteBuffer.wrap(png, 20, 4).getInt();
                logoIndex = workbook.addPicture(png, Workbook.PICTURE_TYPE_PNG);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to read the logo for Excel files", e);
            }
        }
        return logoIndex;
    }

    /** The logo's width divided by its height. */
    double logoAspectRatio() {
        logo();
        return logoAspectRatio;
    }

    /** Lays out every sheet (widths, logo, print settings) and returns the .xlsx file. */
    public byte[] toBytes() {
        for (BrandedSheet sheet : sheets) {
            sheet.finish();
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.setActiveSheet(0);
            workbook.setSelectedTab(0);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new InvalidOperationException("Unable to generate the Excel file: " + e.getMessage());
        }
    }

    @Override
    public void close() throws IOException {
        workbook.close();
    }
}
