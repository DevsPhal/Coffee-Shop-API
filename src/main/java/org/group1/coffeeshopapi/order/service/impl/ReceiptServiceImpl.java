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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ReceiptServiceImpl implements ReceiptService {

    // 80 mm paper. The margins keep the content inside the 72 mm most thermal heads can print.
    private static final float PAGE_WIDTH = 227f;
    private static final float MARGIN = 14f;
    private static final float CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2;

    // Black only: thermal printers dither grey into speckles, so hierarchy comes from size and weight.
    private static final float STORE_SIZE = 15f;
    private static final float TOTAL_SIZE = 13f;
    private static final float BODY_SIZE = 9f;
    private static final float SMALL_SIZE = 8f;

    private static final float LINE_SPACING = 1.4f;
    // Khmer stacks vowels above and subscript consonants below the letters, so its rows get more room.
    private static final float OUTLINE_LINE_SPACING = 1.7f;
    private static final float DIVIDER_HEIGHT = 11f;
    private static final float ITEM_GAP = 4f;
    private static final float COLUMN_GAP = 10f;
    private static final float QUANTITY_GAP = 5f;
    private static final String EXTRA_MARK = "+ ";
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

    private static final Pattern EMPTY_BRACKETS = Pattern.compile("[(\\[{（【]\\s*[)\\]}）】]");
    private static final Pattern DANGLING_SEPARATORS = Pattern.compile("^[\\s\\-–—|·•,/:;~_*.]+|[\\s\\-–—|·•,/:;~_*]+$");

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

            // One page as long as the receipt, so the roll never breaks a page inside an item.
            PDPage page = new PDPage(new PDRectangle(PAGE_WIDTH, height));
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float top = height - MARGIN;
                for (int i = 0; i < rows.size(); i++) {
                    float rowHeight = heights.get(i);
                    drawLine(cs, rows.get(i), top - rowHeight / 2f);
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

    private List<ReceiptLine> buildLines(OrderResponse order, ReceiptLabels labels, boolean invoice) throws IOException {
        List<ReceiptLine> lines = new ArrayList<>();

        lines.add(ReceiptLine.center(titleCase(orDefault(bakongProperties.getStoreLabel(), "590st Cafe")), FONT_BOLD, STORE_SIZE));
        String city = bakongProperties.getMerchantCity();
        if (city != null && !city.isBlank()) {
            lines.add(ReceiptLine.center(titleCase(city), FONT, SMALL_SIZE));
        }
        lines.add(ReceiptLine.space(4));
        lines.add(ReceiptLine.center(labels.title(invoice), FONT_BOLD, BODY_SIZE));
        lines.add(ReceiptLine.divider());

        addOrderDetails(lines, order, labels);
        addItems(lines, order, labels);
        addTotals(lines, order, labels);

        String note = clean(order.note());
        if (!note.isEmpty()) {
            lines.add(ReceiptLine.divider());
            lines.add(ReceiptLine.left(labels.note(), FONT_BOLD, BODY_SIZE, 0));
            lines.add(ReceiptLine.left(note, FONT, BODY_SIZE, 0));
        }
        lines.add(ReceiptLine.divider());
        lines.add(ReceiptLine.center(labels.thanks(), labels.khmer() ? FONT : FONT_ITALIC, BODY_SIZE));
        return lines;
    }

    // Only the details this order has: no empty labels, dashes or brackets for what's missing.
    private void addOrderDetails(List<ReceiptLine> lines, OrderResponse order, ReceiptLabels labels) {
        lines.add(ReceiptLine.field(labels.orderNumber(), "#" + shortId(order.id())));
        if (order.paidAt() != null) {
            lines.add(ReceiptLine.field(labels.date(), labels.formatDate(order.paidAt())));
        }
        String staff = titleCase(cleanName(order.handledByName()));
        if (!staff.isEmpty()) {
            String role = order.handledByRole() != null ? labels.role(order.handledByRole()) + " " : "";
            lines.add(ReceiptLine.field(labels.servedBy(), role + staff));
        }
        if (order.tableNumber() != null && !order.tableNumber().isBlank()) {
            lines.add(ReceiptLine.field(labels.table(), order.tableNumber().strip()));
        }

        String customer = titleCase(cleanName(order.customerName()));
        if (customer.isEmpty()) {
            customer = titleCase(cleanName(order.contactName()));
        }
        if (!customer.isEmpty()) {
            lines.add(ReceiptLine.field(labels.customer(), customer));
        } else if (order.customerId() == null) {
            lines.add(ReceiptLine.field(labels.customer(), labels.walkInCustomer()));
        }
        String phone = cleanName(order.contactPhone());
        if (!phone.isEmpty()) {
            lines.add(ReceiptLine.field(labels.phone(), phone));
        }
        String address = clean(order.deliveryAddress());
        if (order.fulfillmentMethod() == FulfillmentMethod.DELIVERY && !address.isEmpty()) {
            lines.add(ReceiptLine.field(labels.address(), address));
        }
        lines.add(ReceiptLine.divider());
    }

    // Quantities sit in their own column so every product name, option and continuation line starts
    // at the same edge, and every line total lines up on the right.
    private void addItems(List<ReceiptLine> lines, OrderResponse order, ReceiptLabels labels) throws IOException {
        float quantityColumn = 0;
        for (OrderItemResponse item : order.items()) {
            quantityColumn = Math.max(quantityColumn, width(quantity(item), FONT, BODY_SIZE));
        }
        quantityColumn += QUANTITY_GAP;
        float detailWidth = CONTENT_WIDTH - quantityColumn;

        for (int i = 0; i < order.items().size(); i++) {
            OrderItemResponse item = order.items().get(i);
            if (i > 0) {
                lines.add(ReceiptLine.space(ITEM_GAP));
            }
            String englishName = titleCase(clean(item.productName()));
            String khmerName = clean(item.productNameKh());
            // Each language leads with its own product name and keeps the other one underneath.
            String name = labels.khmer() && !khmerName.isEmpty() ? khmerName : englishName;
            String otherName = labels.khmer() ? (khmerName.isEmpty() ? "" : englishName) : khmerName;
            String unitPrice = item.quantity() > 1 ? item.quantity() + " × " + usd(item.unitPrice()) : null;

            lines.add(ReceiptLine.item(quantity(item), name, usd(item.subtotal()), quantityColumn));
            if (!otherName.isEmpty() || unitPrice != null) {
                lines.add(ReceiptLine.twoColumn(otherName, unitPrice, FONT, SMALL_SIZE, quantityColumn));
            }
            for (String row : pack(options(item, labels), " · ", SMALL_SIZE, detailWidth)) {
                lines.add(ReceiptLine.left(row, FONT, SMALL_SIZE, quantityColumn));
            }
            // Every row of extras starts with "+", so a wrapped one still reads as an extra, not an option.
            float extrasWidth = detailWidth - width(EXTRA_MARK, FONT, SMALL_SIZE);
            for (String row : pack(extras(item), ", ", SMALL_SIZE, extrasWidth)) {
                lines.add(ReceiptLine.left(EXTRA_MARK + row, FONT, SMALL_SIZE, quantityColumn));
            }
        }
    }

    private void addTotals(List<ReceiptLine> lines, OrderResponse order, ReceiptLabels labels) {
        if (order.deliveryFee() != null) {
            lines.add(ReceiptLine.divider());
            lines.add(ReceiptLine.twoColumn(labels.subtotal(), usd(order.itemsTotal()), FONT, BODY_SIZE, 0));
            lines.add(ReceiptLine.twoColumn(labels.deliveryFee(), usd(order.deliveryFee()), FONT, BODY_SIZE, 0));
        }
        lines.add(ReceiptLine.rule());
        lines.add(ReceiptLine.twoColumn(labels.total(), usd(order.totalAmount()), FONT_BOLD, TOTAL_SIZE, 0));
        lines.add(ReceiptLine.rule());

        PaymentMethod method = order.paymentMethod();
        if (method == null) {
            return;
        }
        lines.add(ReceiptLine.field(labels.paymentMethod(), labels.payment(method)));
        if (method == PaymentMethod.CASH) {
            if (order.amountTendered() != null) {
                lines.add(ReceiptLine.twoColumn(labels.amountReceived(),
                        money(order.amountTendered(), order.amountTenderedCurrency()), FONT, BODY_SIZE, 0));
            }
            if (order.changeDue() != null) {
                lines.add(ReceiptLine.twoColumn(labels.change(),
                        money(order.changeDue(), order.changeCurrency()), FONT, BODY_SIZE, 0));
            }
        } else if (order.bakongAmount() != null) {
            lines.add(ReceiptLine.twoColumn(labels.amountPaid(),
                    money(order.bakongAmount(), order.bakongCurrency()), FONT, BODY_SIZE, 0));
        }
    }

    private List<String> options(OrderItemResponse item, ReceiptLabels labels) {
        List<String> options = new ArrayList<>();
        if (item.variantName() != null) {
            options.add(labels.variant(item.variantName()));
        }
        if (item.sugarLevel() != null) {
            options.add(labels.sugar(item.sugarLevel()));
        }
        if (item.iceLevel() != null) {
            options.add(labels.ice(item.iceLevel()));
        }
        if (item.milkType() != null) {
            options.add(labels.milk(item.milkType()));
        }
        return options;
    }

    private List<String> extras(OrderItemResponse item) {
        List<String> extras = new ArrayList<>();
        for (OrderItemExtraResponse extra : item.extras()) {
            extras.add(titleCase(clean(extra.name())));
        }
        return extras;
    }

    private String quantity(OrderItemResponse item) {
        return item.quantity() + "×";
    }

    // Splits each line into the rows it needs so nothing runs past the paper edge or into the amount column.
    private List<ReceiptLine> fitToPaper(List<ReceiptLine> lines) throws IOException {
        List<ReceiptLine> rows = new ArrayList<>();
        for (ReceiptLine line : lines) {
            switch (line.type()) {
                case DIVIDER, RULE, SPACE -> rows.add(line);
                case CENTER, LEFT -> {
                    float available = CONTENT_WIDTH - line.indent();
                    for (String row : breakLines(printable(line.left()), line.font(), line.size(), available)) {
                        rows.add(line.withText(null, row, null));
                    }
                }
                case TWO_COLUMN -> {
                    String left = printable(line.left());
                    String right = printable(line.right());
                    float available = CONTENT_WIDTH - line.indent() - width(right, line.font(), line.size()) - COLUMN_GAP;
                    String first = breakLines(left, line.font(), line.size(), available).get(0);
                    rows.add(line.withText(line.prefix(), first, right));
                    // Only the first row carries the amount; a long name continues underneath it at full width.
                    String rest = left.substring(first.length()).strip();
                    if (!rest.isEmpty()) {
                        for (String row : breakLines(rest, line.font(), line.size(), CONTENT_WIDTH - line.indent())) {
                            rows.add(ReceiptLine.left(row, line.font(), line.size(), line.indent()));
                        }
                    }
                }
                case FIELD -> {
                    // The label stays on one line; a long value (an address) wraps, every row ending at the right edge.
                    String label = printable(line.left());
                    float available = CONTENT_WIDTH - width(label, line.font(), line.size()) - COLUMN_GAP;
                    List<String> values = breakLines(printable(line.right()), line.font(), line.size(), available);
                    for (int i = 0; i < values.size(); i++) {
                        rows.add(line.withText(null, i == 0 ? label : "", values.get(i)));
                    }
                }
            }
        }
        return rows;
    }

    private float heightOf(ReceiptLine row) {
        return switch (row.type()) {
            case SPACE -> row.size();
            case DIVIDER, RULE -> DIVIDER_HEIGHT;
            default -> row.size() * (needsOutlines(row.prefix()) || needsOutlines(row.left()) || needsOutlines(row.right())
                    ? OUTLINE_LINE_SPACING : LINE_SPACING);
        };
    }

    private void drawLine(PDPageContentStream cs, ReceiptLine line, float middle) throws IOException {
        switch (line.type()) {
            case SPACE -> {
            }
            case DIVIDER -> {
                cs.saveGraphicsState();
                cs.setLineWidth(0.5f);
                cs.setLineDashPattern(new float[]{2f, 2f}, 0);
                cs.moveTo(MARGIN, middle);
                cs.lineTo(PAGE_WIDTH - MARGIN, middle);
                cs.stroke();
                cs.restoreGraphicsState();
            }
            case RULE -> {
                cs.saveGraphicsState();
                cs.setLineWidth(0.9f);
                cs.moveTo(MARGIN, middle);
                cs.lineTo(PAGE_WIDTH - MARGIN, middle);
                cs.stroke();
                cs.restoreGraphicsState();
            }
            default -> {
                // Centres the letters on the row, whatever its height.
                float y = middle - line.size() * 0.34f;
                if (line.prefix() != null) {
                    drawText(cs, line.prefix(), line.font(), line.size(), MARGIN, y);
                }
                if (line.type() == LineType.CENTER) {
                    float width = width(line.left(), line.font(), line.size());
                    drawText(cs, line.left(), line.font(), line.size(), (PAGE_WIDTH - width) / 2f, y);
                    return;
                }
                drawText(cs, line.left(), line.font(), line.size(), MARGIN + line.indent(), y);
                if (line.right() != null && !line.right().isEmpty()) {
                    float width = width(line.right(), line.font(), line.size());
                    drawText(cs, line.right(), line.font(), line.size(), PAGE_WIDTH - MARGIN - width, y);
                }
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

    // Lays short phrases (drink options, extras) out in rows, keeping each phrase whole on one row, so a
    // line never ends in the middle of "Extra Shot".
    private List<String> pack(List<String> phrases, String separator, float size, float maxWidth) throws IOException {
        List<String> rows = new ArrayList<>();
        String row = "";
        for (String phrase : phrases) {
            String text = clean(phrase);
            if (text.isEmpty()) {
                continue;
            }
            String candidate = row.isEmpty() ? text : row + separator + text;
            if (width(candidate, FONT, size) <= maxWidth) {
                row = candidate;
                continue;
            }
            if (!row.isEmpty()) {
                rows.add(row);
            }
            List<String> pieces = breakLines(text, FONT, size, maxWidth);
            rows.addAll(pieces.subList(0, pieces.size() - 1));
            row = pieces.get(pieces.size() - 1);
        }
        if (!row.isEmpty()) {
            rows.add(row);
        }
        return rows;
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

    // A person's name or phone as it should print. Display names often carry emoji, as in "Nem Sophal (🌸)";
    // the emoji can't print, so the brackets and separators left around nothing go too.
    private String cleanName(String value) {
        return DANGLING_SEPARATORS.matcher(clean(value)).replaceAll("").strip();
    }

    // printable() without brackets that end up empty once the emoji inside them are dropped.
    private String clean(String value) {
        String text = printable(value);
        String previous;
        do {
            previous = text;
            text = EMPTY_BRACKETS.matcher(text).replaceAll("").replaceAll(" {2,}", " ").replaceAll(" ?\n ?", "\n").strip();
        } while (!text.equals(previous));
        return text;
    }

    // Text as the receipt can print it: accents composed, every kind of space a plain one, line breaks kept.
    // Emoji are dropped on every machine, so the preview matches the printout whatever fonts are installed,
    // along with characters no font here can draw, which used to print as "?".
    private String printable(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFC).replace("\r\n", "\n").replace('\r', '\n');
        int[] codePoints = normalized.codePoints().toArray();
        StringBuilder sb = new StringBuilder(normalized.length());
        for (int i = 0; i < codePoints.length; i++) {
            int codePoint = codePoints[i];
            Face face = faceOf(codePoint);
            boolean joinsEmoji = codePoint == ZERO_WIDTH_JOINER
                    && ((i > 0 && isEmoji(codePoints[i - 1])) || (i + 1 < codePoints.length && isEmoji(codePoints[i + 1])));
            if (codePoint == '\n') {
                sb.append('\n');
            } else if (Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint) || Character.isISOControl(codePoint)) {
                sb.append(' ');
            } else if (face == Face.HELVETICA || (face != null && !isEmoji(codePoint) && !joinsEmoji)) {
                sb.appendCodePoint(codePoint);
            }
        }
        return sb.toString().replaceAll(" {2,}", " ").replaceAll(" ?\n ?", "\n").strip();
    }

    // Pictographs plus the pieces emoji are built from: skin tones, flag letters, keycaps, variation selectors.
    private static boolean isEmoji(int codePoint) {
        return Character.isExtendedPictographic(codePoint)
                || Character.isEmojiModifier(codePoint)
                || (codePoint >= 0x1F1E6 && codePoint <= 0x1F1FF)
                || codePoint == 0xFE0E || codePoint == 0xFE0F || codePoint == 0x20E3
                || (codePoint >= 0xE0020 && codePoint <= 0xE007F);
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

    // Helvetica for everything it can encode; the Khmer face, drawn as outlines, for the rest.
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

    private String titleCase(String text) {
        if (text == null || text.isBlank()) {
            return "";
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
        return amount == null ? "-" : String.format(Locale.ENGLISH, "$%,.2f", amount.setScale(2, RoundingMode.HALF_UP));
    }

    private String khr(BigDecimal amount) {
        return amount == null ? "-" : String.format(Locale.ENGLISH, "%,.0f KHR", amount.setScale(0, RoundingMode.HALF_UP));
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

    // TWO_COLUMN wraps its left text beside a fixed amount; FIELD keeps its label and wraps the value.
    private enum LineType { CENTER, LEFT, TWO_COLUMN, FIELD, DIVIDER, RULE, SPACE }

    private record ReceiptLine(LineType type, String prefix, String left, String right, PDType1Font font, float size,
                               float indent) {
        static ReceiptLine center(String text, PDType1Font font, float size) {
            return new ReceiptLine(LineType.CENTER, null, text, null, font, size, 0);
        }

        static ReceiptLine left(String text, PDType1Font font, float size, float indent) {
            return new ReceiptLine(LineType.LEFT, null, text, null, font, size, indent);
        }

        static ReceiptLine twoColumn(String left, String right, PDType1Font font, float size, float indent) {
            return new ReceiptLine(LineType.TWO_COLUMN, null, left, right, font, size, indent);
        }

        // A product row: the quantity at the margin, the name in the column after it, the line total on the right.
        static ReceiptLine item(String quantity, String name, String amount, float nameColumn) {
            return new ReceiptLine(LineType.TWO_COLUMN, quantity, name, amount, FONT, BODY_SIZE, nameColumn);
        }

        static ReceiptLine field(String label, String value) {
            return new ReceiptLine(LineType.FIELD, null, label, value, FONT, BODY_SIZE, 0);
        }

        static ReceiptLine divider() {
            return new ReceiptLine(LineType.DIVIDER, null, null, null, null, 0, 0);
        }

        static ReceiptLine rule() {
            return new ReceiptLine(LineType.RULE, null, null, null, null, 0, 0);
        }

        static ReceiptLine space(float points) {
            return new ReceiptLine(LineType.SPACE, null, null, null, null, points, 0);
        }

        ReceiptLine withText(String prefix, String left, String right) {
            return new ReceiptLine(type, prefix, left, right, font, size, indent);
        }
    }
}
