package org.group1.coffeeshopapi.order.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.encoding.GlyphList;
import org.apache.pdfbox.pdmodel.font.encoding.WinAnsiEncoding;
import org.group1.coffeeshopapi.common.enums.Currency;
import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.enums.PaymentMethod;
import org.group1.coffeeshopapi.common.enums.ReceiptLanguage;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.properties.BakongProperties;
import org.group1.coffeeshopapi.order.dto.response.OrderItemResponse;
import org.group1.coffeeshopapi.order.dto.response.OrderResponse;
import org.group1.coffeeshopapi.order.service.OrderService;
import org.group1.coffeeshopapi.order.service.ReceiptService;
import org.springframework.stereotype.Service;

import java.awt.FontFormatException;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReceiptServiceImpl implements ReceiptService {

    private static final float PAGE_WIDTH = 227f;
    private static final float MARGIN = 14f;
    private static final float CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2;
    private static final float LINE_HEIGHT = 14f;
    // Khmer stacks vowels above and subscript consonants below the letters, so its rows get more room.
    private static final float OUTLINE_LINE_SPACING = 1.7f;
    private static final float COLUMN_GAP = 8f;
    private static final float SECONDARY_NAME_INDENT = 10f;
    private static final float DETAIL_INDENT = 6.7f;
    // The Khmer face ships in one weight, so bold Khmer is its outline traced with a stroke this thick (per point of size).
    private static final float BOLD_STROKE = 0.05f;

    private static final PDType1Font FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDType1Font FONT_ITALIC = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

    private static final String KHMER_FONT_RESOURCE = "/font/NotoSansKhmer-VariableFont_wdth,wght.ttf";
    private static final java.awt.Font KHMER_FONT = loadKhmerFont();
    // Draws what neither Helvetica nor the Khmer face covers (Cyrillic, Chinese, ...) with whatever the server has installed.
    private static final java.awt.Font FALLBACK_FONT = new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, 1);
    private static final FontRenderContext FONT_CONTEXT = new FontRenderContext(null, true, true);

    private static final int KHMER_COENG = 0x17D2;
    private static final int ZERO_WIDTH_SPACE = 0x200B;
    private static final int ZERO_WIDTH_NON_JOINER = 0x200C;
    private static final int ZERO_WIDTH_JOINER = 0x200D;

    private final OrderService orderService;
    private final BakongProperties bakongProperties;

    @Override
    public byte[] generateReceiptPdf(UUID orderId) {
        OrderResponse order = orderService.getAny(orderId);
        if (order.status() != OrderStatus.COMPLETED && order.status() != OrderStatus.DELIVERED) {
            throw new InvalidOperationException("Only a completed or delivered order has a receipt");
        }
        return render(buildLines(order, new ReceiptLabels(ReceiptLanguage.EN)));
    }

    @Override
    public byte[] generateInvoicePdf(UUID orderId, ReceiptLanguage language) {
        OrderResponse order = orderService.getAny(orderId);
        if (order.paidAt() == null) {
            throw new InvalidOperationException("This order hasn't been paid yet");
        }
        return render(buildLines(order, new ReceiptLabels(language)));
    }

    private byte[] render(List<ReceiptLine> lines) {
        try (PDDocument document = new PDDocument()) {
            List<ReceiptLine> rows = fitToPaper(lines);
            List<Float> heights = new ArrayList<>(rows.size());
            float height = MARGIN * 2;
            for (ReceiptLine row : rows) {
                float rowHeight = heightOf(row);
                heights.add(rowHeight);
                height += rowHeight;
            }

            PDPage page = new PDPage(new PDRectangle(PAGE_WIDTH, height));
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float top = height - MARGIN;
                for (int i = 0; i < rows.size(); i++) {
                    float rowHeight = heights.get(i);
                    // Taller rows keep the usual baseline position and split their extra room above and below.
                    float baseline = top - (rowHeight - LINE_HEIGHT) / 2f - LINE_HEIGHT * 0.75f;
                    drawLine(cs, rows.get(i), baseline);
                    top -= rowHeight;
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate receipt PDF", e);
        }
    }

    private List<ReceiptLine> buildLines(OrderResponse order, ReceiptLabels labels) {
        List<ReceiptLine> lines = new ArrayList<>();

        lines.add(ReceiptLine.center(titleCase(orDefault(bakongProperties.getStoreLabel(), "590st Cafe")), FONT_BOLD, 12));
        String city = bakongProperties.getMerchantCity();
        if (city != null && !city.isBlank()) {
            lines.add(ReceiptLine.center(titleCase(city), FONT, 9));
        }
        lines.add(ReceiptLine.divider());

        lines.add(ReceiptLine.left(labels.orderNumber(shortId(order.id())), FONT, 9));
        lines.add(ReceiptLine.left(labels.date(order.paidAt()), FONT, 9));
        lines.add(ReceiptLine.left(labels.servedBy() + servedByLabel(order, labels), FONT, 9));
        lines.add(ReceiptLine.left(labels.customer() + customerLabel(order, labels), FONT, 9));
        lines.add(ReceiptLine.divider());

        for (OrderItemResponse item : order.items()) {
            String englishName = titleCase(item.productName());
            String khmerName = item.productNameKh() != null && !item.productNameKh().isBlank() ? item.productNameKh() : null;
            // Each language leads with its own product name and keeps the other one underneath.
            String name = labels.khmer() && khmerName != null ? khmerName : englishName;
            String otherName = labels.khmer() ? (khmerName != null ? englishName : null) : khmerName;

            lines.add(ReceiptLine.twoColumn(item.quantity() + "x " + name, usd(item.subtotal()), FONT, 9));
            if (otherName != null) {
                lines.add(ReceiptLine.left(otherName, FONT, 9, SECONDARY_NAME_INDENT));
            }
            String detail = itemDetail(item, labels);
            if (detail != null) {
                lines.add(ReceiptLine.left(detail, FONT, 8, DETAIL_INDENT));
            }
        }
        lines.add(ReceiptLine.divider());

        if (order.deliveryFee() != null) {
            BigDecimal itemsSubtotal = order.items().stream()
                    .map(OrderItemResponse::subtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            lines.add(ReceiptLine.twoColumn(labels.subtotal(), usd(itemsSubtotal), FONT, 9));
            lines.add(ReceiptLine.twoColumn(labels.deliveryFee(), usd(order.deliveryFee()), FONT, 9));
        }
        lines.add(ReceiptLine.twoColumn(labels.total(), usd(order.totalAmount()), FONT_BOLD, 11));
        lines.add(ReceiptLine.left(labels.paymentMethod(order.paymentMethod()), FONT, 9));
        if (order.paymentMethod() == PaymentMethod.CASH) {
            lines.add(amountLine(labels.tendered(), order.amountTendered(), order.amountTenderedCurrency()));
            lines.add(amountLine(labels.change(), order.changeDue(), order.changeCurrency()));
        }
        if (order.note() != null && !order.note().isBlank()) {
            lines.add(ReceiptLine.left(labels.note() + order.note(), FONT, 9));
        }
        lines.add(ReceiptLine.divider());
        lines.add(ReceiptLine.center(labels.thanks(), FONT_ITALIC, 9));

        return lines;
    }

    private String servedByLabel(OrderResponse order, ReceiptLabels labels) {
        if (order.handledByName() == null || order.handledByName().isBlank()) {
            return "-----";
        }
        String role = order.handledByRole() != null ? labels.role(order.handledByRole()) + " " : "";
        return role + titleCase(order.handledByName());
    }

    private String customerLabel(OrderResponse order, ReceiptLabels labels) {
        return order.customerName() != null && !order.customerName().isBlank()
                ? titleCase(order.customerName())
                : labels.walkInCustomer();
    }

    // Splits each line into the rows it needs so nothing runs past the paper edge or into the amount column.
    private List<ReceiptLine> fitToPaper(List<ReceiptLine> lines) throws IOException {
        List<ReceiptLine> rows = new ArrayList<>();
        for (ReceiptLine line : lines) {
            if (line.type() == LineType.DIVIDER) {
                rows.add(line);
                continue;
            }
            String left = printable(line.left());
            if (line.type() != LineType.TWO_COLUMN) {
                for (String row : breakLines(left, line.font(), line.size(), CONTENT_WIDTH - line.indent())) {
                    rows.add(new ReceiptLine(line.type(), row, null, line.font(), line.size(), line.indent()));
                }
                continue;
            }

            String right = printable(line.right());
            float available = CONTENT_WIDTH - line.indent() - width(right, line.font(), line.size()) - COLUMN_GAP;
            String first = breakLines(left, line.font(), line.size(), available).get(0);
            rows.add(new ReceiptLine(LineType.TWO_COLUMN, first, right, line.font(), line.size(), line.indent()));
            // Only the first row carries the amount; a long name continues underneath it, indented.
            String rest = left.substring(first.length()).strip();
            if (!rest.isEmpty()) {
                float indent = line.indent() + SECONDARY_NAME_INDENT;
                for (String row : breakLines(rest, line.font(), line.size(), CONTENT_WIDTH - indent)) {
                    rows.add(ReceiptLine.left(row, line.font(), line.size(), indent));
                }
            }
        }
        return rows;
    }

    private float heightOf(ReceiptLine row) {
        if (row.type() == LineType.DIVIDER || !(needsOutlines(row.left()) || needsOutlines(row.right()))) {
            return LINE_HEIGHT;
        }
        return Math.max(LINE_HEIGHT, row.size() * OUTLINE_LINE_SPACING);
    }

    private void drawLine(PDPageContentStream cs, ReceiptLine line, float y) throws IOException {
        switch (line.type()) {
            case DIVIDER -> {
                cs.setLineWidth(0.5f);
                cs.moveTo(MARGIN, y + LINE_HEIGHT * 0.3f);
                cs.lineTo(PAGE_WIDTH - MARGIN, y + LINE_HEIGHT * 0.3f);
                cs.stroke();
            }
            case CENTER -> {
                float width = width(line.left(), line.font(), line.size());
                drawText(cs, line.left(), line.font(), line.size(), (PAGE_WIDTH - width) / 2f, y);
            }
            case LEFT -> drawText(cs, line.left(), line.font(), line.size(), MARGIN + line.indent(), y);
            case TWO_COLUMN -> {
                drawText(cs, line.left(), line.font(), line.size(), MARGIN + line.indent(), y);
                float width = width(line.right(), line.font(), line.size());
                drawText(cs, line.right(), line.font(), line.size(), PAGE_WIDTH - MARGIN - width, y);
            }
        }
    }

    private void drawText(PDPageContentStream cs, String value, PDType1Font font, float size, float x, float y) throws IOException {
        for (TextRun run : runs(value)) {
            if (run.face() == Face.HELVETICA) {
                text(cs, font, size, x, y, run.text());
                x += font.getStringWidth(run.text()) / 1000f * size;
            } else {
                TextLayout layout = outlineLayout(run, size);
                fillOutline(cs, layout, x, y, font == FONT_BOLD ? size * BOLD_STROKE : 0f);
                x += layout.getAdvance();
            }
        }
    }

    private void text(PDPageContentStream cs, PDFont font, float size, float x, float y, String value) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(value);
        cs.endText();
    }

    // Khmer needs its letters reordered and stacked, which PDF text can't do, so AWT shapes it and the
    // resulting glyph outlines are drawn as vector paths. They print the same on any printer or viewer.
    private void fillOutline(PDPageContentStream cs, TextLayout layout, float x, float y, float strokeWidth) throws IOException {
        AffineTransform transform = new AffineTransform();
        transform.translate(x, y);
        transform.scale(1, -1);
        Shape outline = layout.getOutline(transform);
        if (outline.getBounds2D().isEmpty()) {
            return;
        }

        cs.saveGraphicsState();
        if (strokeWidth > 0) {
            cs.setLineWidth(strokeWidth);
            cs.setLineJoinStyle(1);
        }
        PathIterator path = outline.getPathIterator(null);
        float[] coords = new float[6];
        float currentX = 0;
        float currentY = 0;
        while (!path.isDone()) {
            switch (path.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO -> {
                    cs.moveTo(coords[0], coords[1]);
                    currentX = coords[0];
                    currentY = coords[1];
                }
                case PathIterator.SEG_LINETO -> {
                    cs.lineTo(coords[0], coords[1]);
                    currentX = coords[0];
                    currentY = coords[1];
                }
                case PathIterator.SEG_QUADTO -> {
                    // PDF has no quadratic curves; this is the same curve written as a cubic one.
                    cs.curveTo(
                            currentX + 2f / 3f * (coords[0] - currentX), currentY + 2f / 3f * (coords[1] - currentY),
                            coords[2] + 2f / 3f * (coords[0] - coords[2]), coords[3] + 2f / 3f * (coords[1] - coords[3]),
                            coords[2], coords[3]);
                    currentX = coords[2];
                    currentY = coords[3];
                }
                case PathIterator.SEG_CUBICTO -> {
                    cs.curveTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
                    currentX = coords[4];
                    currentY = coords[5];
                }
                case PathIterator.SEG_CLOSE -> cs.closePath();
            }
            path.next();
        }
        if (strokeWidth > 0) {
            cs.fillAndStroke();
        } else {
            cs.fill();
        }
        cs.restoreGraphicsState();
    }

    private TextLayout outlineLayout(TextRun run, float size) {
        java.awt.Font font = run.face() == Face.KHMER ? KHMER_FONT : FALLBACK_FONT;
        return new TextLayout(run.text(), font.deriveFont(size), FONT_CONTEXT);
    }

    private float width(String value, PDType1Font font, float size) throws IOException {
        float width = 0;
        for (TextRun run : runs(value)) {
            width += run.face() == Face.HELVETICA
                    ? font.getStringWidth(run.text()) / 1000f * size
                    : outlineLayout(run, size).getAdvance();
        }
        return width;
    }

    // Splits text into rows no wider than maxWidth: at spaces where possible, otherwise between letters
    // (never inside a Khmer cluster), since Khmer names are often written without any spaces.
    private List<String> breakLines(String value, PDType1Font font, float size, float maxWidth) throws IOException {
        List<String> rows = new ArrayList<>();
        for (String paragraph : value.split("\n")) {
            String row = "";
            for (String word : paragraph.split(" ")) {
                String candidate = row.isEmpty() ? word : row + " " + word;
                if (width(candidate, font, size) <= maxWidth) {
                    row = candidate;
                    continue;
                }
                if (width(word, font, size) <= maxWidth) {
                    rows.add(row);
                    row = word;
                    continue;
                }
                // A word too long for any row starts right where it is and is cut as it reaches the edge.
                row = candidate;
                while (width(row, font, size) > maxWidth) {
                    int cut = fittingCut(row, font, size, maxWidth);
                    rows.add(row.substring(0, cut).strip());
                    row = row.substring(cut).strip();
                }
            }
            if (!row.isEmpty()) {
                rows.add(row);
            }
        }
        if (rows.isEmpty()) {
            rows.add("");
        }
        return rows;
    }

    // Where to cut a word that is too wide: after the last zero-width space that fits when there is one,
    // otherwise as late as fits. Always takes at least one cluster so a row is never empty.
    private int fittingCut(String value, PDType1Font font, float size, float maxWidth) throws IOException {
        int cut = 0;
        int softCut = 0;
        for (int i = value.offsetByCodePoints(0, 1); i < value.length(); i = value.offsetByCodePoints(i, 1)) {
            if (!canBreakBefore(value, i)) {
                continue;
            }
            if (cut > 0 && width(value.substring(0, i), font, size) > maxWidth) {
                break;
            }
            cut = i;
            if (value.codePointBefore(i) == ZERO_WIDTH_SPACE) {
                softCut = i;
            }
        }
        if (softCut > 0) {
            return softCut;
        }
        return cut > 0 ? cut : value.length();
    }

    private boolean canBreakBefore(String value, int index) {
        int previous = value.codePointBefore(index);
        int next = value.codePointAt(index);
        int type = Character.getType(next);
        return previous != KHMER_COENG && previous != ZERO_WIDTH_JOINER
                && next != ZERO_WIDTH_JOINER && next != ZERO_WIDTH_NON_JOINER
                && type != Character.NON_SPACING_MARK
                && type != Character.COMBINING_SPACING_MARK
                && type != Character.ENCLOSING_MARK;
    }

    // Text as the receipt can print it: accents composed, every kind of space a plain one, line breaks
    // kept, and characters no available font can draw (emoji on the server) left out instead of "?".
    private String printable(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFC).replace("\r\n", "\n").replace('\r', '\n');
        StringBuilder sb = new StringBuilder(normalized.length());
        normalized.codePoints().forEach(codePoint -> {
            if (codePoint == '\n') {
                sb.append('\n');
            } else if (Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint) || Character.isISOControl(codePoint)) {
                sb.append(' ');
            } else if (faceOf(codePoint) != null) {
                sb.appendCodePoint(codePoint);
            }
        });
        return sb.toString().replaceAll(" {2,}", " ").replaceAll(" ?\n ?", "\n").strip();
    }

    private boolean needsOutlines(String value) {
        return value != null && value.codePoints().anyMatch(codePoint -> faceOf(codePoint) != Face.HELVETICA);
    }

    private List<TextRun> runs(String value) {
        List<TextRun> runs = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        Face currentFace = null;
        for (int i = 0; i < value.length(); i += Character.charCount(value.codePointAt(i))) {
            int codePoint = value.codePointAt(i);
            Face face = faceOf(codePoint);
            if (face != currentFace && !current.isEmpty()) {
                runs.add(new TextRun(current.toString(), currentFace));
                current.setLength(0);
            }
            current.appendCodePoint(codePoint);
            currentFace = face;
        }
        if (!current.isEmpty()) {
            runs.add(new TextRun(current.toString(), currentFace));
        }
        return runs;
    }

    // Helvetica for everything it can encode, so English receipts look exactly as before.
    private static Face faceOf(int codePoint) {
        if (WinAnsiEncoding.INSTANCE.contains(GlyphList.getAdobeGlyphList().codePointToName(codePoint))) {
            return Face.HELVETICA;
        }
        if (KHMER_FONT.canDisplay(codePoint)) {
            return Face.KHMER;
        }
        return FALLBACK_FONT.canDisplay(codePoint) ? Face.FALLBACK : null;
    }

    private static java.awt.Font loadKhmerFont() {
        try (InputStream in = ReceiptServiceImpl.class.getResourceAsStream(KHMER_FONT_RESOURCE)) {
            return java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, in);
        } catch (IOException | FontFormatException e) {
            throw new IllegalStateException("Failed to load Khmer font", e);
        }
    }

    private String itemDetail(OrderItemResponse item, ReceiptLabels labels) {
        List<String> parts = new ArrayList<>();
        if (item.variantName() != null) {
            parts.add(labels.variant(item.variantName()));
        }
        if (item.sugarLevel() != null) {
            parts.add(labels.sugar(item.sugarLevel()));
        }
        if (item.iceLevel() != null) {
            parts.add(labels.ice(item.iceLevel()));
        }
        if (item.milkType() != null) {
            parts.add(labels.milk(item.milkType()));
        }
        if (!item.extras().isEmpty()) {
            parts.add("+ " + item.extras().stream()
                    .map(extra -> titleCase(extra.name()))
                    .collect(Collectors.joining(", ")));
        }
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    private String titleCase(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String[] words = text.trim().toLowerCase(Locale.ROOT).split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }

    private String usd(BigDecimal amount) {
        return amount == null ? "-" : "$" + amount.setScale(2, RoundingMode.HALF_UP);
    }

    private String khr(BigDecimal amount) {
        return amount == null ? "-" : amount.setScale(0, RoundingMode.HALF_UP) + " KHR";
    }

    private ReceiptLine amountLine(String label, BigDecimal amount, Currency currency) {
        String value = currency == Currency.KHR ? khr(amount) : usd(amount);
        String text = currency != null ? label + " (" + currency + ")" : label;
        return ReceiptLine.twoColumn(text, value, FONT, 9);
    }

    private String shortId(UUID id) {
        return id.toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private enum Face { HELVETICA, KHMER, FALLBACK }

    private record TextRun(String text, Face face) {
    }

    private enum LineType { CENTER, LEFT, TWO_COLUMN, DIVIDER }

    private record ReceiptLine(LineType type, String left, String right, PDType1Font font, float size, float indent) {
        static ReceiptLine center(String text, PDType1Font font, float size) {
            return new ReceiptLine(LineType.CENTER, text, null, font, size, 0);
        }

        static ReceiptLine left(String text, PDType1Font font, float size) {
            return left(text, font, size, 0);
        }

        static ReceiptLine left(String text, PDType1Font font, float size, float indent) {
            return new ReceiptLine(LineType.LEFT, text, null, font, size, indent);
        }

        static ReceiptLine twoColumn(String left, String right, PDType1Font font, float size) {
            return new ReceiptLine(LineType.TWO_COLUMN, left, right, font, size, 0);
        }

        static ReceiptLine divider() {
            return new ReceiptLine(LineType.DIVIDER, null, null, null, 0, 0);
        }
    }
}
