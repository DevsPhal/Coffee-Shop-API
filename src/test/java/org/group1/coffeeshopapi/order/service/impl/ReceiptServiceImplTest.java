package org.group1.coffeeshopapi.order.service.impl;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.group1.coffeeshopapi.common.enums.OrderType;
import org.group1.coffeeshopapi.common.enums.Currency;
import org.group1.coffeeshopapi.common.enums.FulfillmentMethod;
import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.enums.PaymentMethod;
import org.group1.coffeeshopapi.common.enums.ReceiptLanguage;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.enums.SugarLevel;
import org.group1.coffeeshopapi.common.enums.VariantLabel;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.properties.BakongProperties;
import org.group1.coffeeshopapi.order.dto.response.OrderItemResponse;
import org.group1.coffeeshopapi.order.dto.response.OrderResponse;
import org.group1.coffeeshopapi.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReceiptServiceImplTest {

    @Mock private OrderService orderService;
    @Mock private BakongProperties bakongProperties;
    @InjectMocks private ReceiptServiceImpl service;

    @Test
    void aCompletedPickupOrderGetsAReceipt() {
        UUID orderId = UUID.randomUUID();
        when(orderService.getAny(orderId)).thenReturn(order(OrderStatus.COMPLETED, LocalDateTime.now()));

        byte[] pdf = service.generateReceiptPdf(orderId);

        assertThat(pdf).isNotEmpty();
    }

    @Test
    void aDeliveredDeliveryOrderAlsoGetsAReceipt() {
        UUID orderId = UUID.randomUUID();
        when(orderService.getAny(orderId)).thenReturn(order(OrderStatus.DELIVERED, LocalDateTime.now()));

        byte[] pdf = service.generateReceiptPdf(orderId);

        assertThat(pdf).isNotEmpty();
    }

    @Test
    void anOrderStillInProgressHasNoReceiptYet() {
        UUID orderId = UUID.randomUUID();
        when(orderService.getAny(orderId)).thenReturn(order(OrderStatus.OUT_FOR_DELIVERY, LocalDateTime.now()));

        assertThatThrownBy(() -> service.generateReceiptPdf(orderId))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void aPaidOrderStillBeingPreparedAlreadyGetsAnInvoice() {
        UUID orderId = UUID.randomUUID();
        when(orderService.getAny(orderId)).thenReturn(order(OrderStatus.PREPARING, LocalDateTime.now()));

        byte[] pdf = service.generateInvoicePdf(orderId, ReceiptLanguage.EN);

        assertThat(pdf).isNotEmpty();
    }

    @Test
    void anUnpaidOrderHasNoInvoiceYet() {
        UUID orderId = UUID.randomUUID();
        when(orderService.getAny(orderId)).thenReturn(order(OrderStatus.PENDING, null));

        assertThatThrownBy(() -> service.generateInvoicePdf(orderId, ReceiptLanguage.EN))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("hasn't been paid");
    }

    @Test
    void aProductWithAKhmerNameStillProducesAReceipt() {
        UUID orderId = UUID.randomUUID();
        OrderItemResponse item = new OrderItemResponse(
                UUID.randomUUID(), UUID.randomUUID(), "Iced Coffee", "កាហ្វេទឹកកក",
                1, new BigDecimal("1.50"), new BigDecimal("1.50"),
                null, null, null, null, List.of(), null, null);
        when(orderService.getAny(orderId))
                .thenReturn(order(OrderStatus.COMPLETED, LocalDateTime.now(), List.of(item)));

        byte[] pdf = service.generateReceiptPdf(orderId);

        assertThat(pdf).isNotEmpty();
    }

    @Test
    void namesInKhmerOrWithAccentsArePrintedInsteadOfQuestionMarks() throws IOException {
        PaidOrder order = new PaidOrder();
        order.customerName = "សុខ ដារ៉ា";
        order.staffName = "josé";
        order.note = "Ring twice — thanks";

        String text = invoice(order, ReceiptLanguage.EN);

        assertThat(text).contains("Customer", "Barista José", "Ring twice — thanks").doesNotContain("?");
    }

    @Test
    void emojiInADisplayNameLeavesNoEmptyBracketsOrSeparatorsBehind() throws IOException {
        for (String displayName : List.of("Nem Sophal (🌸)", "Nem Sophal ( 🌸✨ )", "Nem Sophal | 💕", "👑 Nem Sophal 👑")) {
            PaidOrder order = new PaidOrder();
            order.customerName = displayName;
            order.contactPhone = " ";

            for (ReceiptLanguage language : ReceiptLanguage.values()) {
                String customerRow = row(invoice(order, language), language == ReceiptLanguage.EN ? "Customer" : "Nem");
                assertThat(customerRow).as("%s in %s", displayName, language).endsWith("Nem Sophal")
                        .doesNotContain("(", ")", "|");
            }
        }
    }

    @Test
    void aWalkInOrderSaysSoAndSkipsTheDetailsItDoesNotHave() throws IOException {
        PaidOrder order = new PaidOrder();
        order.customerId = null;
        order.customerName = null;
        order.staffName = null;

        String text = invoice(order, ReceiptLanguage.EN);

        assertThat(row(text, "Customer")).endsWith("Walk-in Customer");
        assertThat(text).doesNotContain("Served by", "Table", "Phone", "Address", "-----", "null");
    }

    @Test
    void tablePhoneAndAddressArePrintedOnlyWhereTheOrderHasThem() throws IOException {
        PaidOrder dineIn = new PaidOrder();
        dineIn.fulfillment = FulfillmentMethod.DINE_IN;
        dineIn.tableNumber = "A3";
        // A pickup order keeps an old address it no longer delivers to.
        dineIn.deliveryAddress = "#12, St. 590, Toul Kork, Phnom Penh";
        PaidOrder delivery = new PaidOrder();
        delivery.fulfillment = FulfillmentMethod.DELIVERY;
        delivery.deliveryFee = new BigDecimal("1.00");
        delivery.contactPhone = "+855 12 345 678";
        delivery.deliveryAddress = "#12, St. 590, Toul Kork, Phnom Penh";

        String dineInText = invoice(dineIn, ReceiptLanguage.EN);
        String deliveryText = invoice(delivery, ReceiptLanguage.EN);

        assertThat(row(dineInText, "Table")).endsWith("A3");
        assertThat(dineInText).doesNotContain("Phone", "Address", "Toul Kork");
        assertThat(row(deliveryText, "Phone")).endsWith("+855 12 345 678");
        assertThat(deliveryText).contains("Address", "#12, St. 590", "Toul Kork").doesNotContain("Table");
    }

    @Test
    void englishAndKhmerInvoicesPrintTheSameOrderNumberAndAmounts() throws IOException {
        PaidOrder order = new PaidOrder();
        order.fulfillment = FulfillmentMethod.DELIVERY;
        order.deliveryFee = new BigDecimal("1.00");

        String english = invoice(order, ReceiptLanguage.EN);
        String khmer = invoice(order, ReceiptLanguage.KH);

        assertThat(figures(khmer)).isEqualTo(figures(english))
                .contains("$5.00", "$2.50", "$10.00", "$1.00", "$11.00", "40,000 KHR", "2,000 KHR");
        assertThat(english).contains("2 × $2.50", "Large · Less Sugar", "Subtotal", "Delivery fee", "TOTAL",
                "Payment method", "Amount received", "Change", "Thank you, come again!");
        assertThat(khmer).doesNotContain("Subtotal", "TOTAL", "Payment method", "Amount received", "Customer", "Thank you");
    }

    @Test
    void aPosReceiptPrintsTheOrderTypeAndNoDeliveryFee() throws IOException {
        PaidOrder takeAway = new PaidOrder();
        takeAway.customerId = null;
        takeAway.customerName = null;
        takeAway.orderType = OrderType.TAKE_AWAY;
        // POS orders saved before order types existed still carry a $0.00 fee.
        takeAway.deliveryFee = BigDecimal.ZERO;
        PaidOrder dineIn = new PaidOrder();
        dineIn.customerId = null;
        dineIn.customerName = null;
        dineIn.orderType = OrderType.DINE_IN;
        dineIn.fulfillment = FulfillmentMethod.DINE_IN;
        dineIn.tableNumber = "A3";

        String takeAwayText = invoice(takeAway, ReceiptLanguage.EN);
        String dineInText = invoice(dineIn, ReceiptLanguage.EN);
        String khmer = invoice(takeAway, ReceiptLanguage.KH);

        assertThat(row(takeAwayText, "Order type")).endsWith("Take away");
        assertThat(takeAwayText).doesNotContain("Delivery fee", "Subtotal", "Table");
        assertThat(row(dineInText, "Order type")).endsWith("Dine in");
        assertThat(row(dineInText, "Table")).endsWith("A3");
        // PDF text extraction can't read Khmer glyphs, so check the English labels were replaced.
        assertThat(khmer).doesNotContain("Order type", "Take away", "Delivery fee", "Subtotal");
    }

    @Test
    void anOnlineOrderHasNoOrderTypeRow() throws IOException {
        String text = invoice(new PaidOrder(), ReceiptLanguage.EN);

        assertThat(text).doesNotContain("Order type");
    }

    @Test
    void cashPaidInDollarsShowsDollarsAndNoSubtotalWithoutADeliveryFee() throws IOException {
        PaidOrder order = new PaidOrder();
        order.tendered = new BigDecimal("20.00");
        order.tenderedCurrency = Currency.USD;
        order.change = new BigDecimal("10.00");
        order.changeCurrency = Currency.USD;

        String text = invoice(order, ReceiptLanguage.EN);

        assertThat(row(text, "Amount received")).endsWith("$20.00");
        assertThat(row(text, "Change")).endsWith("$10.00");
        assertThat(text).doesNotContain("Subtotal", "KHR");
    }

    @Test
    void bakongInvoicesShowTheAmountPaidInTheCurrencyPaid() throws IOException {
        PaidOrder order = new PaidOrder();
        order.paymentMethod = PaymentMethod.BAKONG;
        order.tendered = null;
        order.change = null;
        order.bakongAmount = new BigDecimal("41000");
        order.bakongCurrency = Currency.KHR;

        String text = invoice(order, ReceiptLanguage.EN);

        assertThat(row(text, "Payment method")).endsWith("Bakong KHQR");
        assertThat(row(text, "Amount paid")).endsWith("41,000 KHR");
        assertThat(text).doesNotContain("Amount received", "Change");
    }

    @Test
    void cashInRielPrintsTheRoundedRielTotalAndTheRateSoTheChangeAddsUp() throws IOException {
        PaidOrder order = new PaidOrder();
        order.tendered = new BigDecimal("45000");
        order.tenderedCurrency = Currency.KHR;
        order.change = new BigDecimal("4000");
        order.changeCurrency = Currency.KHR;
        order.exchangeRate = new BigDecimal("4100.0000");

        String text = invoice(order, ReceiptLanguage.EN);

        assertThat(row(text, "TOTAL")).endsWith("$10.00");
        assertThat(row(text, "Total in KHR")).endsWith("41,000 KHR");
        assertThat(row(text, "Exchange rate")).endsWith("1 USD = 4,100 KHR");
        assertThat(row(text, "Amount received")).endsWith("45,000 KHR");
        assertThat(row(text, "Change")).endsWith("4,000 KHR");
    }

    @Test
    void theRielTotalOnTheReceiptFollowsTheNearest100Rule() throws IOException {
        PaidOrder roundsUp = new PaidOrder();
        roundsUp.fulfillment = FulfillmentMethod.DELIVERY;
        roundsUp.deliveryFee = new BigDecimal("0.19");   // $10.19 = 41,779 riel -> 41,800
        roundsUp.exchangeRate = new BigDecimal("4100");
        PaidOrder roundsDown = new PaidOrder();
        roundsDown.fulfillment = FulfillmentMethod.DELIVERY;
        roundsDown.deliveryFee = new BigDecimal("0.50");  // $10.50 = 43,050 riel -> 43,000
        roundsDown.exchangeRate = new BigDecimal("4100");

        assertThat(row(invoice(roundsUp, ReceiptLanguage.EN), "Total in KHR")).endsWith("41,800 KHR");
        assertThat(row(invoice(roundsDown, ReceiptLanguage.EN), "Total in KHR")).endsWith("43,000 KHR");
    }

    @Test
    void aRielKhqrReceiptShowsTheRateNextToTheAmountPaid() throws IOException {
        PaidOrder order = new PaidOrder();
        order.paymentMethod = PaymentMethod.BAKONG;
        order.tendered = null;
        order.change = null;
        order.bakongAmount = new BigDecimal("41000");
        order.bakongCurrency = Currency.KHR;
        order.exchangeRate = new BigDecimal("4100");

        String text = invoice(order, ReceiptLanguage.EN);
        String khmer = invoice(order, ReceiptLanguage.KH);

        assertThat(row(text, "Amount paid")).endsWith("41,000 KHR");
        assertThat(row(text, "Exchange rate")).endsWith("1 USD = 4,100 KHR");
        assertThat(khmer).contains("1 USD = 4,100 KHR").doesNotContain("Exchange rate");
    }

    @Test
    void longNamesAndNotesWrapInsideThePaperMarginsInBothLanguages() throws IOException {
        String longKhmer = "កាហ្វេទឹកដោះគោទឹកកក".repeat(4);
        PaidOrder order = new PaidOrder();
        order.items = List.of(new OrderItemResponse(
                UUID.randomUUID(), UUID.randomUUID(), "Caramel Macchiato With Extra Whipped Cream And Hazelnut Syrup",
                longKhmer, 12, new BigDecimal("3.90"), new BigDecimal("46.80"),
                VariantLabel.LARGE, SugarLevel.LESS, null, null, List.of(), null, null));
        order.customerName = "Customer With A Remarkably Long Telegram Display Name";
        order.deliveryAddress = "#12, St. 590, Boeng Kak 2, Toul Kork, Phnom Penh, near the big blue gate";
        order.fulfillment = FulfillmentMethod.DELIVERY;
        order.note = "Please leave the order with the security guard at the front gate.\n" + longKhmer;
        UUID orderId = order.id;
        when(orderService.getAny(orderId)).thenReturn(order.build());

        for (ReceiptLanguage language : ReceiptLanguage.values()) {
            BufferedImage page = image(service.generateInvoicePdf(orderId, language));
            // The 14pt margins at 2 px per point, less a point of slack for anti-aliased edges.
            int margin = 26;
            List<String> inkInMargins = new ArrayList<>();
            for (int y = 0; y < page.getHeight(); y++) {
                for (int x = 0; x < page.getWidth(); x++) {
                    boolean inMargin = x < margin || x >= page.getWidth() - margin;
                    if (inMargin && page.getRaster().getSample(x, y, 0) < 200) {
                        inkInMargins.add(x + "," + y);
                    }
                }
            }
            assertThat(inkInMargins).as(language.name()).isEmpty();
        }
    }

    @Test
    void theReceiptOpensWithTheLogoAndClosesWithTheThanksInTheScriptFace() throws IOException {
        PaidOrder order = new PaidOrder();
        when(orderService.getAny(order.id)).thenReturn(order.build());
        byte[] pdf = service.generateInvoicePdf(order.id, ReceiptLanguage.EN);

        assertThat(text(pdf)).contains("INVOICE", "Thank you, come again!").doesNotContain("590st Cafe");
        try (PDDocument document = Loader.loadPDF(pdf)) {
            List<String> fonts = new ArrayList<>();
            for (var name : document.getPage(0).getResources().getFontNames()) {
                fonts.add(document.getPage(0).getResources().getFont(name).getName());
            }
            assertThat(fonts).anyMatch(font -> font.contains("Satisfy"));
            assertThat(document.getPage(0).getResources().getXObjectNames()).hasSize(1);
        }
        // The wordmark fills the top-centre of the paper with ink.
        BufferedImage page = image(pdf);
        int ink = 0;
        for (int y = 28; y < 140; y++) {
            for (int x = page.getWidth() / 2 - 60; x < page.getWidth() / 2 + 60; x++) {
                if (page.getRaster().getSample(x, y, 0) < 128) {
                    ink++;
                }
            }
        }
        assertThat(ink).isGreaterThan(2000);
    }

    @Test
    void anAccountNamedAfterItsRolePrintsTheRoleOnce() throws IOException {
        PaidOrder owner = new PaidOrder();
        owner.staffName = "Super Admin";
        owner.staffRole = Role.SUPER_ADMIN;
        PaidOrder barista = new PaidOrder();

        assertThat(row(invoice(owner, ReceiptLanguage.EN), "Served by")).endsWith("Super Admin")
                .doesNotContain("Super Admin Super Admin");
        assertThat(row(invoice(barista, ReceiptLanguage.EN), "Served by")).endsWith("Barista Sokha");
    }

    @Test
    void productCodesKeepTheirLettersAndAPieceIsNotSpelledOut() throws IOException {
        PaidOrder order = new PaidOrder();
        order.items = List.of(
                new OrderItemResponse(UUID.randomUUID(), UUID.randomUUID(), "CROISSANT mwa4x9k7", null,
                        1, new BigDecimal("2.75"), new BigDecimal("2.75"),
                        VariantLabel.PIECE, null, null, null, List.of(), null, null),
                new OrderItemResponse(UUID.randomUUID(), UUID.randomUUID(), "coke 330ML", null,
                        1, new BigDecimal("16.00"), new BigDecimal("16.00"),
                        VariantLabel.CARTON, null, null, null, List.of(), null, null));

        String text = invoice(order, ReceiptLanguage.EN);

        assertThat(text).contains("Croissant mwa4x9k7", "Coke 330ML", "Carton").doesNotContain("Piece");
    }

    @Test
    void theUnitPriceSharesTheFirstOptionsRowWhenThereIsNoSecondName() throws IOException {
        PaidOrder order = new PaidOrder();
        order.items = List.of(new OrderItemResponse(UUID.randomUUID(), UUID.randomUUID(), "Iced Latte", null,
                2, new BigDecimal("2.70"), new BigDecimal("5.40"),
                VariantLabel.LARGE, SugarLevel.LESS, null, null, List.of(), null, null));

        String text = invoice(order, ReceiptLanguage.EN);

        assertThat(row(text, "Large · Less Sugar")).endsWith("2 × $2.70");
    }

    private static String text(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private static BufferedImage image(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFRenderer(document).renderImageWithDPI(0, 144, ImageType.GRAY);
        }
    }

    private String invoice(PaidOrder order, ReceiptLanguage language) throws IOException {
        when(orderService.getAny(order.id)).thenReturn(order.build());
        return text(service.generateInvoicePdf(order.id, language));
    }

    // The printed row that starts with the given label, such as "Customer          Nem Sophal".
    private static String row(String text, String label) {
        return text.lines().map(String::strip).filter(line -> line.startsWith(label)).findFirst()
                .orElseThrow(() -> new AssertionError("No row starting with " + label + " in:\n" + text));
    }

    // The order number and every amount, in the order they are printed.
    private static List<String> figures(String text) {
        return Pattern.compile("#[0-9A-F]{8}|\\$[\\d,]+\\.\\d{2}|[\\d,]+ KHR").matcher(text).results()
                .map(match -> match.group()).toList();
    }

    private static OrderItemResponse latte() {
        return new OrderItemResponse(
                UUID.randomUUID(), UUID.randomUUID(), "Iced Latte", "ឡាតេទឹកកក",
                2, new BigDecimal("2.50"), new BigDecimal("5.00"),
                VariantLabel.LARGE, SugarLevel.LESS, null, null, List.of(), null, null);
    }

    // A paid order with the parts the receipt prints; each test changes only what it is about.
    private static final class PaidOrder {
        final UUID id = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        String customerName = "Dara";
        String staffName = "Sokha";
        Role staffRole = Role.BARISTA;
        String contactPhone;
        String deliveryAddress;
        String tableNumber;
        String note;
        FulfillmentMethod fulfillment = FulfillmentMethod.PICKUP;
        OrderType orderType;
        List<OrderItemResponse> items = List.of(latte(), latte());
        BigDecimal deliveryFee;
        PaymentMethod paymentMethod = PaymentMethod.CASH;
        BigDecimal tendered = new BigDecimal("40000");
        Currency tenderedCurrency = Currency.KHR;
        BigDecimal change = new BigDecimal("2000");
        Currency changeCurrency = Currency.KHR;
        BigDecimal bakongAmount;
        Currency bakongCurrency;
        BigDecimal exchangeRate;

        OrderResponse build() {
            BigDecimal itemsTotal = items.stream().map(OrderItemResponse::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal total = deliveryFee != null ? itemsTotal.add(deliveryFee) : itemsTotal;
            LocalDateTime paidAt = LocalDateTime.of(2026, 11, 7, 15, 45);
            return new OrderResponse(
                    id, null, staffName, staffRole, customerId, customerName,
                    OrderStatus.COMPLETED, items, total,
                    fulfillment, orderType, null, tableNumber, deliveryAddress, null, contactPhone, null, null,
                    paymentMethod, tendered, tenderedCurrency, change, changeCurrency,
                    null, null, bakongCurrency, bakongAmount,
                    note, paidAt, paidAt, paidAt,
                    null, null, deliveryFee, null, null, false, itemsTotal, null, null, exchangeRate,
                    null, null, null, null, null);
        }
    }

    private OrderResponse order(OrderStatus status, LocalDateTime paidAt) {
        return order(status, paidAt, List.of());
    }

    private OrderResponse order(OrderStatus status, LocalDateTime paidAt, List<OrderItemResponse> items) {
        return new OrderResponse(
                UUID.randomUUID(), null, null, null, null, null,
                status, items, new BigDecimal("10.00"),
                FulfillmentMethod.PICKUP, null, null, null, null, null, null, null, null,
                PaymentMethod.CASH, new BigDecimal("10.00"), Currency.USD, BigDecimal.ZERO, Currency.USD,
                null, null, null, null,
                null, paidAt, LocalDateTime.now(), LocalDateTime.now(),
                null, null, null, null, null, false, new BigDecimal("10.00"), null, null, null,
                null, null, null, null, null);
    }
}
