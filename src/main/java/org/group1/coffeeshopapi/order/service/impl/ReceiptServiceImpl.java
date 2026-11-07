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
import org.group1.coffeeshopapi.common.enums.FulfillmentMethod;
import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.enums.PaymentMethod;
import org.group1.coffeeshopapi.common.enums.ReceiptLanguage;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.properties.BakongProperties;
import org.group1.coffeeshopapi.order.dto.response.OrderItemExtraResponse;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ReceiptServiceImpl implements ReceiptService {

    private static final float PAGE_WIDTH = 227f;
    private static final float MARGIN = 14f;
    private static final float CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2;
    // Row height per point of type size. Khmer stacks vowels above and subscript consonants below the
    // letters, so its rows get more room.
    private static final float LINE_SPACING = 1.5f;
    private static final float OUTLINE_LINE_SPACING = 1.75f;
    private static final float RULE_HEIGHT = 9f;
    private static final float TOTAL_RULE_WIDTH = 1f;
    private static final float SECTION_GAP = 4f;
    private static final float COLUMN_GAP = 8f;
    private static final float QUANTITY_GAP = 6f;
    private static final float STACKED_VALUE_INDENT = 8f;
    // The Khmer face ships in one weight, so bold Khmer is its outline traced with a stroke this thick (per point of size).
    private static final float BOLD_STROKE = 0.05f;

    // One type scale for the whole receipt, so each level of the hierarchy reads the same everywhere.
    private static final float STORE_SIZE = 14f;
    private static final float TOTAL_SIZE = 13f;
    private static final float TITLE_SIZE = 10f;
    private static final float TEXT_SIZE = 9f;
    private static final float SECONDARY_SIZE = 8.5f;
    private static final float DETAIL_SIZE = 8f;

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
    // Characters that draw as nothing, which people use to pad Telegram display names (Braille blank, Hangul fillers).
    private static final Set<Integer> BLANK_LOOKALIKES = Set.of(0x2800, 0x3164, 0x115F, 0x1160, 0xFFA0);
    // Brackets with nothing printable left inside, such as what remains of "(🌸)" once the emoji is dropped.
    private static final Pattern EMPTY_BRACKETS = Pattern.compile("[(\\[{（]\\s*[-–—_,;:·|/*~]*\\s*[)\\]}）]");
    private static final Pattern DANGLING_SEPARATORS = Pattern.compile("^[\\s\\-–—_,;:·|/]+|[\\s\\-–—_,;:·|/]+$");

    private final OrderService orderService;
    private final BakongProperties bakongProperties;

    @Override
    public byte[] generateReceiptPdf(UUID orderId) {
        OrderResponse order = orderService.getAny(orderId);
        if (order.status() != OrderStatus.COMPLETED && order.status() != OrderStatus.DELIVERED) {
            throw new InvalidOperationException("Only a completed or delivered order has a receipt");
        }
        return render(order, new ReceiptLabels(ReceiptLanguage.EN), false);
    }

    @Override
    public byte[] generateInvoicePdf(UUID orderId, ReceiptLanguage language) {
        OrderResponse order = orderService.getAny(orderId);
        if (order.paidAt() == null) {
            throw new InvalidOperationException("This order hasn't been paid yet");
        }
        return render(order, new ReceiptLabels(language), true);
    }

    private byte[] render(OrderResponse order, ReceiptLabels labels, boolean invoice) {
        try (PDDocument document = new PDDocument()) {
            List<ReceiptLine> rows = fitToPaper(buildLines(order, labels, invoice));
            List<Float> heights = new ArrayList<>(rows.size());
            float height = MARGIN * 2;
            for (ReceiptLine row : rows) {
                float rowHeight = heightOf(row);
                heights.add(rowHeight);
                height += rowHeight;
            }

            // One page as long as the receipt, so the printer never breaks it between pages.
            PDPage page = new PDPage(new PDRectangle(PAGE_WIDTH, height));
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float top = height - MARGIN;
                for (int i = 0; i < rows.size(); i++) {
                    drawLine(cs, rows.get(i), top, heights.get(i));
                    top -= heights.get(i);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate receipt PDF", e);
        }
    }

    private List<ReceiptLine> buildLines(OrderResponse order, ReceiptLabels labels, boolean invoice) throws IOException {
        List<ReceiptLine> lines = new ArrayList<>();

        lines.add(ReceiptLine.center(titleCase(orDefault(bakongProperties.getStoreLabel(), "590st Cafe")), FONT_BOLD, STORE_SIZE));
        String city = bakongProperties.getMerchantCity();
        if (city != null && !city.isBlank()) {
            lines.add(ReceiptLine.center(titleCase(city), FONT, TEXT_SIZE));
        }
        lines.add(ReceiptLine.gap(SECTION_GAP));
        lines.add(ReceiptLine.center(labels.title(invoice), FONT_BOLD, TITLE_SIZE));
        lines.add(ReceiptLine.dashedRule());

        lines.add(field(labels.orderNumber(), "#" + shortId(order.id())));
        LocalDateTime at = order.paidAt() != null ? order.paidAt() : order.createdAt();
        if (at != null) {
            lines.add(field(labels.date(), labels.formatDate(at)));
        }
        String staff = displayName(order.handledByName());
        if (staff != null) {
            String role = order.handledByRole() != null ? labels.role(order.handledByRole()) + " " : "";
            lines.add(field(labels.servedBy(), role + staff));
        }
        lines.add(field(labels.customer(), customerName(order, labels)));
        if (!isBlank(order.tableNumber())) {
            lines.add(field(labels.table(), order.tableNumber().strip()));
        }
        if (!isBlank(order.contactPhone())) {
            lines.add(field(labels.phone(), formatPhone(order.contactPhone())));
        }
        if (order.fulfillmentMethod() == FulfillmentMethod.DELIVERY && !isBlank(order.deliveryAddress())) {
            lines.add(field(labels.address(), order.deliveryAddress()));
        }
        lines.add(ReceiptLine.dashedRule());

        float nameIndent = quantityColumn(order.items());
        for (int i = 0; i < order.items().size(); i++) {
            OrderItemResponse item = order.items().get(i);
            if (i > 0) {
                lines.add(ReceiptLine.gap(SECTION_GAP));
            }
            String englishName = displayName(item.productName());
            String khmerName = displayName(item.productNameKh());
            // Each language leads with its own product name and keeps the other one underneath.
            String name = labels.khmer() && khmerName != null ? khmerName : englishName;
            String otherName = labels.khmer() ? englishName : khmerName;
            if (name == null) {
                name = orDefault(otherName, "-");
                otherName = null;
            }

            lines.add(ReceiptLine.item(quantity(item), name, usd(item.subtotal()), nameIndent));
            if (otherName != null && !otherName.equals(name)) {
                lines.add(ReceiptLine.left(otherName, FONT, SECONDARY_SIZE, nameIndent));
            }
            String options = itemOptions(item, labels);
            if (options != null) {
                lines.add(ReceiptLine.left(options, FONT, DETAIL_SIZE, nameIndent));
            }
            String extras = itemExtras(item);
            if (extras != null) {
                lines.add(ReceiptLine.left(extras, FONT, DETAIL_SIZE, nameIndent));
            }
            if (item.quantity() != null && item.quantity() > 1 && item.unitPrice() != null) {
                lines.add(ReceiptLine.left(labels.unitPrice(usd(item.unitPrice())), FONT, DETAIL_SIZE, nameIndent));
            }
        }
        if (order.deliveryFee() != null) {
            lines.add(ReceiptLine.dashedRule());
            lines.add(field(labels.subtotal(), usd(order.itemsTotal())));
            lines.add(field(labels.deliveryFee(), usd(order.deliveryFee())));
        }
        lines.add(ReceiptLine.rule(TOTAL_RULE_WIDTH));
        lines.add(ReceiptLine.keyValue(labels.total(), usd(order.totalAmount()), FONT_BOLD, TOTAL_SIZE));
        lines.add(ReceiptLine.rule(TOTAL_RULE_WIDTH));

        if (order.paymentMethod() != null) {
            lines.add(field(labels.paymentMethod(), labels.payment(order.paymentMethod())));
        }
        if (order.paymentMethod() == PaymentMethod.CASH) {
            if (order.amountTendered() != null) {
                lines.add(field(labels.amountReceived(), money(order.amountTendered(), order.amountTenderedCurrency())));
            }
            if (order.changeDue() != null) {
                lines.add(field(labels.change(), money(order.changeDue(), order.changeCurrency())));
            }
        } else if (order.paymentMethod() == PaymentMethod.BAKONG
                && order.bakongAmount() != null && order.bakongCurrency() == Currency.KHR) {
            // Only a riel KHQR adds anything: a dollar one paid exactly the total above.
            lines.add(field(labels.amountPaid(), money(order.bakongAmount(), order.bakongCurrency())));
        }

        if (!isBlank(order.note())) {
            lines.add(ReceiptLine.dashedRule());
            lines.add(ReceiptLine.left(labels.note(), FONT_BOLD, TEXT_SIZE, 0));
            lines.add(ReceiptLine.left(order.note(), FONT, TEXT_SIZE, 0));
        }
        lines.add(ReceiptLine.dashedRule());
        lines.add(ReceiptLine.center(labels.thanks(), FONT_ITALIC, TEXT_SIZE));

        return lines;
    }

    private ReceiptLine field(String label, String value) {
        return ReceiptLine.keyValue(label, value, FONT, TEXT_SIZE);
    }

    // The account's name, else the delivery contact's, else a walk-in.
    private String customerName(OrderResponse order, ReceiptLabels labels) {
        String name = displayName(order.customerName());
        if (name == null) {
            name = displayName(order.contactName());
        }
        return name != null ? name : labels.walkInCustomer();
    }

    // A name as the receipt prints it, or null when nothing printable is left: drawable characters only,
    // and no brackets or separators emptied by what was dropped, so a Telegram name like
    // "Nem Sophal (🌸)" prints as "Nem Sophal" and not "Nem Sophal ( )".
    private String displayName(String raw) {
        String name = printable(raw).replace('\n', ' ');
        String previous;
        do {
            previous = name;
            name = EMPTY_BRACKETS.matcher(name).replaceAll(" ");
        } while (!name.equals(previous));
        name = DANGLING_SEPARATORS.matcher(name.replaceAll(" {2,}", " ")).replaceAll("");
        return name.isBlank() ? null : titleCase(name);
    }

    // The dashboard's grouping (012 345 678); a number in any other shape prints as it was entered.
    private String formatPhone(String raw) {
        String digits = raw.replaceAll("\\D", "");
        if (digits.startsWith("855") && digits.length() > 8) {
            String national = digits.substring(3);
            digits = national.startsWith("0") ? national : "0" + national;
        }
        if (!digits.matches("0\\d{8,9}")) {
            return raw.strip();
        }
        return digits.substring(0, 3) + " " + digits.substring(3, 6) + " " + digits.substring(6);
    }

    private String quantity(OrderItemResponse item) {
        return item.quantity() + "×";
    }

    // Item names start at one x for the whole list, just past the widest quantity, so they line up.
    private float quantityColumn(List<OrderItemResponse> items) throws IOException {
        float widest = 0;
        for (OrderItemResponse item : items) {
            widest = Math.max(widest, FONT.getStringWidth(quantity(item)) / 1000f * TEXT_SIZE);
        }
        return widest + QUANTITY_GAP;
    }

    // Splits each line into the rows it needs so nothing runs past the paper edge or into the amount column.
    private List<ReceiptLine> fitToPaper(List<ReceiptLine> lines) throws IOException {
        List<ReceiptLine> rows = new ArrayList<>();
        for (ReceiptLine line : lines) {
            switch (line.type()) {
                case RULE, DASHED_RULE, GAP -> rows.add(line);
                case CENTER, LEFT -> {
                    float available = CONTENT_WIDTH - line.indent();
                    for (String row : breakLines(printable(line.left()), line.font(), line.size(), available)) {
                        rows.add(line.with(row, null));
                    }
                }
                case KEY_VALUE -> fitField(line, rows);
                case ITEM -> fitItem(line, rows);
            }
        }
        return rows;
    }

    // A label and its value share a row when they fit; otherwise the value goes underneath, indented, so a
    // long name or address wraps instead of crowding the label.
    private void fitField(ReceiptLine line, List<ReceiptLine> rows) throws IOException {
        String label = printable(line.left());
        String value = printable(line.right()).replace('\n', ' ');
        if (width(label, line.font(), line.size()) + COLUMN_GAP + width(value, line.font(), line.size()) <= CONTENT_WIDTH) {
            rows.add(line.with(label, value));
            return;
        }
        rows.add(ReceiptLine.left(label, line.font(), line.size(), 0));
        for (String row : breakLines(value, line.font(), line.size(), CONTENT_WIDTH - STACKED_VALUE_INDENT)) {
            rows.add(ReceiptLine.left(row, line.font(), line.size(), STACKED_VALUE_INDENT));
        }
    }

    // The quantity, the start of the name and the price share the first row; the rest of a long name
    // continues under the name, clear of the price column.
    private void fitItem(ReceiptLine line, List<ReceiptLine> rows) throws IOException {
        String name = printable(line.left());
        String price = printable(line.right());
        float available = CONTENT_WIDTH - line.indent() - width(price, line.font(), line.size()) - COLUMN_GAP;
        String first = breakLines(name, line.font(), line.size(), available).get(0);
        rows.add(line.with(first, price));
        String rest = name.substring(first.length()).strip();
        if (!rest.isEmpty()) {
            for (String row : breakLines(rest, line.font(), line.size(), CONTENT_WIDTH - line.indent())) {
                rows.add(ReceiptLine.left(row, line.font(), line.size(), line.indent()));
            }
        }
    }

    private float heightOf(ReceiptLine row) {
        return switch (row.type()) {
            case RULE, DASHED_RULE -> RULE_HEIGHT;
            case GAP -> row.size();
            default -> row.size() * (needsOutlines(row.left()) || needsOutlines(row.right())
                    ? OUTLINE_LINE_SPACING : LINE_SPACING);
        };
    }

    private void drawLine(PDPageContentStream cs, ReceiptLine row, float top, float height) throws IOException {
        // Centers the capitals in the row.
        float baseline = top - height / 2f - row.size() * 0.36f;
        switch (row.type()) {
            case RULE, DASHED_RULE -> {
                float y = top - height / 2f;
                cs.saveGraphicsState();
                cs.setLineWidth(row.type() == LineType.RULE ? row.size() : 0.5f);
                if (row.type() == LineType.DASHED_RULE) {
                    cs.setLineDashPattern(new float[]{2f, 1.5f}, 0);
                }
                cs.moveTo(MARGIN, y);
                cs.lineTo(PAGE_WIDTH - MARGIN, y);
                cs.stroke();
                cs.restoreGraphicsState();
            }
            case GAP -> {
            }
            case CENTER -> {
                float width = width(row.left(), row.font(), row.size());
                drawText(cs, row.left(), row.font(), row.size(), (PAGE_WIDTH - width) / 2f, baseline);
            }
            case LEFT -> drawText(cs, row.left(), row.font(), row.size(), MARGIN + row.indent(), baseline);
            case KEY_VALUE, ITEM -> {
                if (row.lead() != null) {
                    drawText(cs, row.lead(), row.font(), row.size(), MARGIN, baseline);
                }
                drawText(cs, row.left(), row.font(), row.size(), MARGIN + row.indent(), baseline);
                float width = width(row.right(), row.font(), row.size());
                drawText(cs, row.right(), row.font(), row.size(), PAGE_WIDTH - MARGIN - width, baseline);
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
    // kept, and invisible characters, emoji and anything no available font can draw left out instead of
    // printed as "?". Emoji go even where a font has them, so a name prints the same on every server.
    private String printable(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFC).replace("\r\n", "\n").replace('\r', '\n');
        StringBuilder sb = new StringBuilder(normalized.length());
        normalized.codePoints().forEach(codePoint -> {
            if (codePoint == '\n') {
                sb.append('\n');
            } else if (Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint)
                    || Character.isISOControl(codePoint) || BLANK_LOOKALIKES.contains(codePoint)) {
                sb.append(' ');
            } else if (faceOf(codePoint) == Face.HELVETICA
                    || (faceOf(codePoint) != null && !isInvisible(codePoint) && !isEmoji(codePoint))) {
                sb.appendCodePoint(codePoint);
            }
        });
        return sb.toString().replaceAll(" {2,}", " ").replaceAll(" ?\n ?", "\n").strip();
    }

    // Soft hyphens, direction marks, emoji variation selectors and the like. The joiners and the zero-width
    // space stay, since Khmer spelling uses them.
    private static boolean isInvisible(int codePoint) {
        boolean format = Character.getType(codePoint) == Character.FORMAT
                && codePoint != ZERO_WIDTH_SPACE && codePoint != ZERO_WIDTH_NON_JOINER && codePoint != ZERO_WIDTH_JOINER;
        return format || (codePoint >= 0xFE00 && codePoint <= 0xFE0F);
    }

    // Emoji and the pieces they are built from (skin tones, flag letters, keycaps). Helvetica's own symbols
    // such as © and ™ are checked before this and still print.
    private static boolean isEmoji(int codePoint) {
        return Character.isExtendedPictographic(codePoint)
                || (Character.isEmojiComponent(codePoint) && codePoint > 0xFF && codePoint != ZERO_WIDTH_JOINER);
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

    // Helvetica for everything it can encode; the Khmer face and the fallback only for the rest.
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

    private String itemOptions(OrderItemResponse item, ReceiptLabels labels) {
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
        return parts.isEmpty() ? null : String.join(" · ", parts);
    }

    private String itemExtras(OrderItemResponse item) {
        List<String> names = new ArrayList<>();
        for (OrderItemExtraResponse extra : item.extras()) {
            String name = displayName(extra.name());
            if (name != null) {
                names.add(name);
            }
        }
        return names.isEmpty() ? null : "+ " + String.join(", ", names);
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

    private String money(BigDecimal amount, Currency currency) {
        return currency == Currency.KHR ? khr(amount) : usd(amount);
    }

    private String usd(BigDecimal amount) {
        return amount == null ? "-" : "$" + String.format(Locale.US, "%,.2f", amount.setScale(2, RoundingMode.HALF_UP));
    }

    private String khr(BigDecimal amount) {
        return amount == null ? "-" : String.format(Locale.US, "%,.0f", amount.setScale(0, RoundingMode.HALF_UP)) + " KHR";
    }

    private String shortId(UUID id) {
        return id.toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private enum Face { HELVETICA, KHMER, FALLBACK }

    private record TextRun(String text, Face face) {
    }

    private enum LineType { CENTER, LEFT, KEY_VALUE, ITEM, RULE, DASHED_RULE, GAP }

    // lead is an item's quantity, drawn at the margin before its name. For a RULE, size is the stroke
    // width; for a GAP, the height.
    private record ReceiptLine(LineType type, String lead, String left, String right, PDType1Font font, float size, float indent) {
        ReceiptLine with(String newLeft, String newRight) {
            return new ReceiptLine(type, lead, newLeft, newRight, font, size, indent);
        }

        static ReceiptLine center(String text, PDType1Font font, float size) {
            return new ReceiptLine(LineType.CENTER, null, text, null, font, size, 0);
        }

        static ReceiptLine left(String text, PDType1Font font, float size, float indent) {
            return new ReceiptLine(LineType.LEFT, null, text, null, font, size, indent);
        }

        static ReceiptLine keyValue(String label, String value, PDType1Font font, float size) {
            return new ReceiptLine(LineType.KEY_VALUE, null, label, value, font, size, 0);
        }

        static ReceiptLine item(String quantity, String name, String price, float nameIndent) {
            return new ReceiptLine(LineType.ITEM, quantity, name, price, FONT, TEXT_SIZE, nameIndent);
        }

        static ReceiptLine rule(float strokeWidth) {
            return new ReceiptLine(LineType.RULE, null, null, null, null, strokeWidth, 0);
        }

        static ReceiptLine dashedRule() {
            return new ReceiptLine(LineType.DASHED_RULE, null, null, null, null, 0, 0);
        }

        static ReceiptLine gap(float height) {
            return new ReceiptLine(LineType.GAP, null, null, null, null, height, 0);
        }
    }
}
