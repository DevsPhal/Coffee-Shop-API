package org.group1.coffeeshopapi.order.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.group1.coffeeshopapi.common.enums.Currency;
import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.enums.PaymentMethod;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReceiptServiceImpl implements ReceiptService {

    // 80mm-wide thermal-receipt page. Height is computed per receipt from its line count.
    private static final float PAGE_WIDTH = 227f;
    private static final float MARGIN = 14f;
    private static final float LINE_HEIGHT = 14f;

    private static final PDType1Font FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDType1Font FONT_ITALIC = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

    // Khmer script needs real shaping (subscript consonants, reordering vowels) that PDFBox can't
    // do on its own, so Khmer text is drawn as filled vector outlines via Java2D instead of as
    // PDFBox glyph text.
    private static final String KHMER_FONT_RESOURCE = "/font/NotoSansKhmer-VariableFont_wdth,wght.ttf";
    private static final java.awt.Font KHMER_FONT = loadKhmerFont();
    private static final float KHMER_INDENT = 10f;

    private static final DateTimeFormatter RECEIPT_DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    private final OrderService orderService;
    private final BakongProperties bakongProperties;

    @Override
    public byte[] generateReceiptPdf(UUID orderId) {
        OrderResponse order = orderService.getAny(orderId);
        if (order.status() != OrderStatus.COMPLETED && order.status() != OrderStatus.DELIVERED) {
            throw new InvalidOperationException("Only a completed or delivered order has a receipt");
        }
        return render(buildLines(order));
    }

    @Override
    public byte[] generateInvoicePdf(UUID orderId) {
        OrderResponse order = orderService.getAny(orderId);
        if (order.paidAt() == null) {
            throw new InvalidOperationException("This order hasn't been paid yet");
        }
        return render(buildLines(order));
    }

    private byte[] render(List<ReceiptLine> lines) {
        float height = MARGIN * 2 + lines.size() * LINE_HEIGHT;

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(PAGE_WIDTH, height));
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float y = height - MARGIN - LINE_HEIGHT * 0.75f;
                for (ReceiptLine line : lines) {
                    drawLine(cs, line, y);
                    y -= LINE_HEIGHT;
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate receipt PDF", e);
        }
    }

    private List<ReceiptLine> buildLines(OrderResponse order) {
        List<ReceiptLine> lines = new ArrayList<>();

        lines.add(ReceiptLine.center(sanitize(titleCase(orDefault(bakongProperties.getStoreLabel(), "590st Cafe"))), FONT_BOLD, 12));
        String city = bakongProperties.getMerchantCity();
        if (city != null && !city.isBlank()) {
            lines.add(ReceiptLine.center(sanitize(titleCase(city)), FONT, 9));
        }
        lines.add(ReceiptLine.divider());

        lines.add(ReceiptLine.left("Order #" + shortId(order.id()), FONT, 9));
        lines.add(ReceiptLine.left("Date: " + (order.paidAt() != null ? order.paidAt().format(RECEIPT_DATE_FORMAT) : "-"), FONT, 9));
        lines.add(ReceiptLine.left("Served By: " + sanitize(servedByLabel(order)), FONT, 9));
        lines.add(ReceiptLine.left("Customer: " + sanitize(customerLabel(order)), FONT, 9));
        lines.add(ReceiptLine.divider());

        for (OrderItemResponse item : order.items()) {
            lines.add(ReceiptLine.twoColumn(
                    sanitize(item.quantity() + "x " + titleCase(item.productName())), usd(item.subtotal()), FONT, 9));
            if (item.productNameKh() != null && !item.productNameKh().isBlank()) {
                lines.add(ReceiptLine.khmer(item.productNameKh(), 9));
            }
            String detail = itemDetail(item);
            if (detail != null) {
                lines.add(ReceiptLine.left("   " + sanitize(detail), FONT, 8));
            }
        }
        lines.add(ReceiptLine.divider());

        if (order.deliveryFee() != null) {
            BigDecimal itemsSubtotal = order.items().stream()
                    .map(OrderItemResponse::subtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            lines.add(ReceiptLine.twoColumn("Subtotal", usd(itemsSubtotal), FONT, 9));
            lines.add(ReceiptLine.twoColumn("Delivery Fee", usd(order.deliveryFee()), FONT, 9));
        }
        lines.add(ReceiptLine.twoColumn("TOTAL", usd(order.totalAmount()), FONT_BOLD, 11));
        lines.add(ReceiptLine.left("Payment Method: " + paymentLabel(order.paymentMethod()), FONT, 9));
        if (order.paymentMethod() == PaymentMethod.CASH) {
            lines.add(amountLine("Tendered", order.amountTendered(), order.amountTenderedCurrency()));
            lines.add(amountLine("Change", order.changeDue(), order.changeCurrency()));
        }
        if (order.note() != null && !order.note().isBlank()) {
            lines.add(ReceiptLine.left("Note: " + sanitize(order.note()), FONT, 9));
        }
        lines.add(ReceiptLine.divider());
        lines.add(ReceiptLine.center("Thank you, come again!", FONT_ITALIC, 9));

        return lines;
    }

    // e.g. "Barista Phal".
    private String servedByLabel(OrderResponse order) {
        if (order.handledByName() == null || order.handledByName().isBlank()) {
            return "-----";
        }
        String role = order.handledByRole() != null ? readable(order.handledByRole().name()) + " " : "";
        return role + titleCase(order.handledByName());
    }

    private String customerLabel(OrderResponse order) {
        return order.customerName() != null && !order.customerName().isBlank()
                ? titleCase(order.customerName())
                : "Walk-in Customer";
    }

    private String paymentLabel(PaymentMethod method) {
        return switch (method) {
            case CASH -> "Cash";
            case BAKONG -> "Bakong KHQR";
        };
    }

    private void drawLine(PDPageContentStream cs, ReceiptLine line, float y) {
        try {
            switch (line.type()) {
                case DIVIDER -> {
                    cs.setLineWidth(0.5f);
                    cs.moveTo(MARGIN, y + LINE_HEIGHT * 0.3f);
                    cs.lineTo(PAGE_WIDTH - MARGIN, y + LINE_HEIGHT * 0.3f);
                    cs.stroke();
                }
                case CENTER -> {
                    float width = line.font().getStringWidth(line.left()) / 1000f * line.size();
                    text(cs, line.font(), line.size(), (PAGE_WIDTH - width) / 2f, y, line.left());
                }
                case LEFT -> text(cs, line.font(), line.size(), MARGIN, y, line.left());
                case KHMER -> drawKhmerText(cs, line.left(), MARGIN + KHMER_INDENT, y, line.size());
                case TWO_COLUMN -> {
                    text(cs, line.font(), line.size(), MARGIN, y, line.left());
                    float width = line.font().getStringWidth(line.right()) / 1000f * line.size();
                    text(cs, line.font(), line.size(), PAGE_WIDTH - MARGIN - width, y, line.right());
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to render receipt line", e);
        }
    }

    private void text(PDPageContentStream cs, PDFont font, float size, float x, float y, String value) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(value);
        cs.endText();
    }

    // Shapes the text with Java2D (which handles Khmer subscripts/reordering correctly) and fills
    // the resulting glyph outlines directly as PDF paths, instead of drawing it as font text.
    private void drawKhmerText(PDPageContentStream cs, String value, float x, float y, float size) throws IOException {
        if (value == null || value.isBlank()) {
            return;
        }
        TextLayout layout = new TextLayout(value, KHMER_FONT.deriveFont(size), new FontRenderContext(null, true, true));
        // PDF page space is y-up; Java2D glyph outlines are y-down, so flip vertically to match.
        AffineTransform transform = new AffineTransform();
        transform.translate(x, y);
        transform.scale(1, -1);
        Shape outline = layout.getOutline(transform);

        PathIterator path = outline.getPathIterator(null, 0.3);
        float[] coords = new float[6];
        while (!path.isDone()) {
            switch (path.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO -> cs.moveTo(coords[0], coords[1]);
                case PathIterator.SEG_LINETO -> cs.lineTo(coords[0], coords[1]);
                case PathIterator.SEG_CLOSE -> cs.closePath();
            }
            path.next();
        }
        cs.fill();
    }

    private static java.awt.Font loadKhmerFont() {
        try (InputStream in = ReceiptServiceImpl.class.getResourceAsStream(KHMER_FONT_RESOURCE)) {
            return java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, in);
        } catch (IOException | FontFormatException e) {
            throw new IllegalStateException("Failed to load Khmer font", e);
        }
    }

    private String itemDetail(OrderItemResponse item) {
        List<String> parts = new ArrayList<>();
        if (item.variantName() != null) {
            parts.add(readable(item.variantName().name()));
        }
        if (item.sugarLevel() != null) {
            parts.add(readable(item.sugarLevel().name()) + " sugar");
        }
        if (item.iceLevel() != null) {
            parts.add(readable(item.iceLevel().name()));
        }
        if (item.milkType() != null) {
            parts.add(readable(item.milkType().name()) + " milk");
        }
        if (!item.extras().isEmpty()) {
            parts.add("+ " + item.extras().stream()
                    .map(extra -> titleCase(extra.name()))
                    .collect(Collectors.joining(", ")));
        }
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    private String readable(String enumName) {
        String[] words = enumName.toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }

    // Normalizes however a name was typed at signup (e.g. "SOPHAL NEM") to "Sophal Nem".
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

    // KHR has no minor unit, so it's shown as a whole number rather than usd()'s 2 decimal places.
    private String khr(BigDecimal amount) {
        return amount == null ? "-" : amount.setScale(0, RoundingMode.HALF_UP) + " KHR";
    }

    // "Tendered (USD): $20.00" or "Change (KHR): 5500 KHR" — label carries the currency so it
    // still reads clearly when tendered and change aren't in the same one.
    private ReceiptLine amountLine(String label, BigDecimal amount, Currency currency) {
        String value = currency == Currency.KHR ? khr(amount) : usd(amount);
        return ReceiptLine.twoColumn(label + " (" + currency + ")", value, FONT, 9);
    }

    private String shortId(UUID id) {
        return id.toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    // These fonts only render plain ASCII — swap out anything else (e.g. Khmer text) rather than
    // failing the whole receipt.
    private String sanitize(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            sb.append(c >= 0x20 && c <= 0x7E ? c : '?');
        }
        return sb.toString();
    }

    private enum LineType { CENTER, LEFT, KHMER, TWO_COLUMN, DIVIDER }

    private record ReceiptLine(LineType type, String left, String right, PDFont font, float size) {
        static ReceiptLine center(String text, PDFont font, float size) {
            return new ReceiptLine(LineType.CENTER, text, null, font, size);
        }

        static ReceiptLine left(String text, PDFont font, float size) {
            return new ReceiptLine(LineType.LEFT, text, null, font, size);
        }

        static ReceiptLine khmer(String text, float size) {
            return new ReceiptLine(LineType.KHMER, text, null, null, size);
        }

        static ReceiptLine twoColumn(String left, String right, PDFont font, float size) {
            return new ReceiptLine(LineType.TWO_COLUMN, left, right, font, size);
        }

        static ReceiptLine divider() {
            return new ReceiptLine(LineType.DIVIDER, null, null, null, 0);
        }
    }
}
