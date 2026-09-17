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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
            String tenderedLabel = order.amountTenderedCurrency() == Currency.KHR ? "Tendered (KHR)" : "Tendered (USD)";
            String tenderedValue = order.amountTenderedCurrency() == Currency.KHR
                    ? khr(order.amountTendered()) : usd(order.amountTendered());
            lines.add(ReceiptLine.twoColumn(tenderedLabel, tenderedValue, FONT, 9));
            lines.add(ReceiptLine.twoColumn("Change", usd(order.changeDue()), FONT, 9));
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

    private enum LineType { CENTER, LEFT, TWO_COLUMN, DIVIDER }

    private record ReceiptLine(LineType type, String left, String right, PDFont font, float size) {
        static ReceiptLine center(String text, PDFont font, float size) {
            return new ReceiptLine(LineType.CENTER, text, null, font, size);
        }

        static ReceiptLine left(String text, PDFont font, float size) {
            return new ReceiptLine(LineType.LEFT, text, null, font, size);
        }

        static ReceiptLine twoColumn(String left, String right, PDFont font, float size) {
            return new ReceiptLine(LineType.TWO_COLUMN, left, right, font, size);
        }

        static ReceiptLine divider() {
            return new ReceiptLine(LineType.DIVIDER, null, null, null, 0);
        }
    }
}
