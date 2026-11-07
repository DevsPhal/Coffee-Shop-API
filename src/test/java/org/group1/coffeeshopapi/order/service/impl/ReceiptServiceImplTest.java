package org.group1.coffeeshopapi.order.service.impl;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.group1.coffeeshopapi.common.enums.Currency;
import org.group1.coffeeshopapi.common.enums.FulfillmentMethod;
import org.group1.coffeeshopapi.common.enums.IceLevel;
import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.enums.PaymentMethod;
import org.group1.coffeeshopapi.common.enums.ReceiptLanguage;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.enums.SugarLevel;
import org.group1.coffeeshopapi.common.enums.VariantLabel;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.properties.BakongProperties;
import org.group1.coffeeshopapi.order.dto.response.OrderItemExtraResponse;
import org.group1.coffeeshopapi.order.dto.response.OrderItemResponse;
import org.group1.coffeeshopapi.order.dto.response.OrderResponse;
import org.group1.coffeeshopapi.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

    private static final LocalDateTime PAID_AT = LocalDateTime.of(2026, 11, 7, 15, 45);

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

    @ParameterizedTest
    @ValueSource(strings = {"Nem Sophal ( )", "Nem Sophal ()", "Nem Sophal (🌸 🌸)", "Nem Sophal (⠀)", "Nem Sophal [ㅤ] -"})
    void aNamePartThatCannotBePrintedLeavesNoEmptyBracketsBehind(String storedName) throws IOException {
        PaidOrder order = new PaidOrder();
        order.customerName = storedName;

        String text = invoiceText(order, ReceiptLanguage.EN);

        assertThat(text).containsPattern("Customer\\s+Nem Sophal\\s").doesNotContain("(", ")", "[", "]", "?");
    }

    @Test
    void namesInKhmerOrWithAccentsArePrintedInsteadOfQuestionMarks() throws IOException {
        PaidOrder order = new PaidOrder();
        order.customerName = "សុខ ដារ៉ា 🙂";
        order.staffName = "josé";
        order.note = "Ring twice — thanks";

        String text = invoiceText(order, ReceiptLanguage.EN);

        assertThat(text).containsPattern("Served by\\s+Barista José").contains("Ring twice — thanks").doesNotContain("?");
    }

    @Test
    void aCustomerWithCompleteDetailsGetsTheirPhoneAndDeliveryAddress() throws IOException {
        PaidOrder order = new PaidOrder();
        order.fulfillment = FulfillmentMethod.DELIVERY;
        order.contactPhone = "+855 12 345 678";
        order.deliveryAddress = "#12, Street 590, Toul Kork";
        order.deliveryFee = new BigDecimal("1.00");

        String text = invoiceText(order, ReceiptLanguage.EN);

        assertThat(text)
                .containsPattern("Customer\\s+Dara")
                .containsPattern("Phone\\s+012 345 678")
                .containsPattern("Address\\s+#12, Street 590, Toul Kork")
                .containsPattern("Subtotal\\s+\\$2\\.50")
                .containsPattern("Delivery fee\\s+\\$1\\.00")
                .containsPattern("TOTAL\\s+\\$3\\.50");
    }

    @Test
    void aDineInOrderShowsItsTable() throws IOException {
        PaidOrder order = new PaidOrder();
        order.fulfillment = FulfillmentMethod.DINE_IN;
        order.tableNumber = "T5";

        assertThat(invoiceText(order, ReceiptLanguage.EN)).containsPattern("Table\\s+T5");
    }

    @Test
    void aCustomerWithOnlyANameGetsNoEmptyRows() throws IOException {
        PaidOrder order = new PaidOrder();
        order.staffName = null;

        String text = invoiceText(order, ReceiptLanguage.EN);

        assertThat(text).containsPattern("Customer\\s+Dara")
                .doesNotContain("Phone", "Table", "Address", "Served by", "Subtotal", "-----");
    }

    @Test
    void anOrderWithoutAnyCustomerIsAWalkIn() throws IOException {
        PaidOrder order = new PaidOrder();
        order.customerName = null;

        assertThat(invoiceText(order, ReceiptLanguage.EN)).containsPattern("Customer\\s+Walk-in Customer");
    }

    @Test
    void aDeliveryWithoutAnAccountIsInTheContactsName() throws IOException {
        PaidOrder order = new PaidOrder();
        order.customerName = null;
        order.contactName = "vanna";

        assertThat(invoiceText(order, ReceiptLanguage.EN)).containsPattern("Customer\\s+Vanna");
    }

    @Test
    void eachItemListsItsOptionsExtrasAndUnitPrice() throws IOException {
        OrderItemResponse latte = new OrderItemResponse(
                UUID.randomUUID(), UUID.randomUUID(), "Iced Latte", "ឡាតេទឹកកក",
                2, new BigDecimal("3.00"), new BigDecimal("6.00"),
                VariantLabel.LARGE, SugarLevel.LESS, IceLevel.LESS_ICE, null,
                List.of(extra("extra shot"), extra("oat milk")), null, null);
        PaidOrder order = new PaidOrder();
        order.items = List.of(latte, croissant());

        String text = invoiceText(order, ReceiptLanguage.EN);

        assertThat(text)
                .containsPattern("2×\\s*Iced Latte\\s+\\$6\\.00")
                .contains("Large · Less Sugar · Less Ice", "+ Extra Shot, Oat Milk", "$3.00 each")
                .containsPattern("1×\\s*Butter Croissant\\s+\\$1\\.75")
                .doesNotContain("$1.75 each");
    }

    @Test
    void cashIsShownInTheCurrencyItWasPaidIn() throws IOException {
        PaidOrder dollars = new PaidOrder();
        dollars.tendered = new BigDecimal("20");
        dollars.change = new BigDecimal("17.50");
        PaidOrder riel = new PaidOrder();
        riel.tendered = new BigDecimal("50000");
        riel.tenderedCurrency = Currency.KHR;
        riel.change = new BigDecimal("39750");
        riel.changeCurrency = Currency.KHR;

        assertThat(invoiceText(dollars, ReceiptLanguage.EN))
                .containsPattern("Payment method\\s+Cash")
                .containsPattern("Amount received\\s+\\$20\\.00")
                .containsPattern("Change\\s+\\$17\\.50");
        assertThat(invoiceText(riel, ReceiptLanguage.EN))
                .containsPattern("Amount received\\s+50,000 KHR")
                .containsPattern("Change\\s+39,750 KHR");
    }

    @Test
    void bakongShowsTheAmountPaidOnlyWhenItWasInRiel() throws IOException {
        PaidOrder riel = new PaidOrder();
        riel.paymentMethod = PaymentMethod.BAKONG;
        riel.bakongAmount = new BigDecimal("10300");
        riel.bakongCurrency = Currency.KHR;
        PaidOrder dollars = new PaidOrder();
        dollars.paymentMethod = PaymentMethod.BAKONG;
        dollars.bakongAmount = new BigDecimal("2.50");
        dollars.bakongCurrency = Currency.USD;

        assertThat(invoiceText(riel, ReceiptLanguage.EN))
                .containsPattern("Payment method\\s+Bakong KHQR")
                .containsPattern("Amount paid\\s+10,300 KHR")
                .doesNotContain("Amount received", "Change");
        assertThat(invoiceText(dollars, ReceiptLanguage.EN)).doesNotContain("Amount paid");
    }

    @Test
    void englishAndKhmerInvoicesPrintTheSameOrderNumberAndAmounts() throws IOException {
        PaidOrder order = new PaidOrder();
        order.items = List.of(latte(2), croissant());
        order.deliveryFee = new BigDecimal("1.00");
        order.tendered = new BigDecimal("40000");
        order.tenderedCurrency = Currency.KHR;
        order.change = new BigDecimal("13000");
        order.changeCurrency = Currency.KHR;
        OrderResponse response = order.build();
        UUID orderId = response.id();
        when(orderService.getAny(orderId)).thenReturn(response);

        String english = text(service.generateInvoicePdf(orderId, ReceiptLanguage.EN));
        String khmer = text(service.generateInvoicePdf(orderId, ReceiptLanguage.KH));

        assertThat(figures(khmer)).isEqualTo(figures(english))
                .contains("$5.00", "$2.50", "$1.75", "$6.75", "$1.00", "$7.75", "40,000 KHR", "13,000 KHR");
        assertThat(english).contains("INVOICE", "Subtotal", "TOTAL", "Payment method", "Thank you, come again!");
        assertThat(khmer).doesNotContain("INVOICE", "Subtotal", "TOTAL", "Payment", "Customer", "each", "Thank you");
    }

    @Test
    void longNamesNotesAndAddressesWrapInsideThePaperMarginsInBothLanguages() throws IOException {
        String longKhmer = "កាហ្វេទឹកដោះគោទឹកកក".repeat(4);
        OrderItemResponse item = new OrderItemResponse(
                UUID.randomUUID(), UUID.randomUUID(), "Caramel Macchiato With Extra Whipped Cream And Hazelnut Syrup",
                longKhmer, 12, new BigDecimal("3.90"), new BigDecimal("46.80"),
                VariantLabel.LARGE, SugarLevel.LESS, IceLevel.EXTRA_ICE, null,
                List.of(extra("whipped cream"), extra("hazelnut syrup"), extra("caramel drizzle")), null, null);
        PaidOrder order = new PaidOrder();
        order.customerName = "Customer With A Remarkably Long Telegram Display Name";
        order.fulfillment = FulfillmentMethod.DELIVERY;
        order.deliveryAddress = "Building 12B, Street 590, Sangkat Boeung Kak II, Khan Toul Kork, Phnom Penh";
        order.contactPhone = "012345678";
        order.items = List.of(item, latte(1));
        order.deliveryFee = new BigDecimal("1.50");
        order.note = "Please leave the order with the security guard at the front gate.\n" + longKhmer;
        OrderResponse response = order.build();
        when(orderService.getAny(response.id())).thenReturn(response);

        for (ReceiptLanguage language : ReceiptLanguage.values()) {
            BufferedImage page = image(service.generateInvoicePdf(response.id(), language));
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

    private String invoiceText(PaidOrder order, ReceiptLanguage language) throws IOException {
        OrderResponse response = order.build();
        when(orderService.getAny(response.id())).thenReturn(response);
        return text(service.generateInvoicePdf(response.id(), language));
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

    // The order number and every amount, in the order they are printed.
    private static List<String> figures(String text) {
        return Pattern.compile("#[0-9A-F]{8}|\\$[\\d,]+\\.\\d{2}|[\\d,]+ KHR").matcher(text).results()
                .map(match -> match.group()).toList();
    }

    private static OrderItemResponse latte(int quantity) {
        BigDecimal unitPrice = new BigDecimal("2.50");
        return new OrderItemResponse(
                UUID.randomUUID(), UUID.randomUUID(), "Iced Latte", "ឡាតេទឹកកក",
                quantity, unitPrice, unitPrice.multiply(BigDecimal.valueOf(quantity)),
                VariantLabel.LARGE, SugarLevel.LESS, null, null, List.of(), null, null);
    }

    private static OrderItemResponse croissant() {
        return new OrderItemResponse(
                UUID.randomUUID(), UUID.randomUUID(), "Butter Croissant", "នំក្រូសង់ប៊ឺ",
                1, new BigDecimal("1.75"), new BigDecimal("1.75"),
                null, null, null, null, List.of(), null, null);
    }

    private static OrderItemExtraResponse extra(String name) {
        return new OrderItemExtraResponse(UUID.randomUUID(), name, new BigDecimal("0.50"), null);
    }

    // A paid order with ordinary defaults; each test changes only what it is about. The totals are
    // derived the way Order does it: items total plus any delivery fee.
    private static final class PaidOrder {
        String customerName = "Dara";
        String contactName;
        String contactPhone;
        String tableNumber;
        String deliveryAddress;
        String staffName = "Sokha";
        FulfillmentMethod fulfillment = FulfillmentMethod.PICKUP;
        List<OrderItemResponse> items = List.of(latte(1));
        BigDecimal deliveryFee;
        PaymentMethod paymentMethod = PaymentMethod.CASH;
        BigDecimal tendered = new BigDecimal("10");
        Currency tenderedCurrency = Currency.USD;
        BigDecimal change = new BigDecimal("7.50");
        Currency changeCurrency = Currency.USD;
        BigDecimal bakongAmount;
        Currency bakongCurrency;
        String note;

        OrderResponse build() {
            BigDecimal itemsTotal = items.stream().map(OrderItemResponse::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal total = deliveryFee != null ? itemsTotal.add(deliveryFee) : itemsTotal;
            boolean cash = paymentMethod == PaymentMethod.CASH;
            return new OrderResponse(
                    UUID.randomUUID(), null, staffName, staffName != null ? Role.BARISTA : null, null, customerName,
                    OrderStatus.COMPLETED, items, total,
                    fulfillment, null, tableNumber, deliveryAddress, contactName, contactPhone, null, null,
                    paymentMethod, cash ? tendered : null, cash ? tenderedCurrency : null,
                    cash ? change : null, cash ? changeCurrency : null,
                    null, null, bakongCurrency, bakongAmount,
                    note, PAID_AT, PAID_AT, PAID_AT,
                    null, null, deliveryFee, null, null, false, itemsTotal, null, null);
        }
    }

    private OrderResponse order(OrderStatus status, LocalDateTime paidAt) {
        return order(status, paidAt, List.of());
    }

    private OrderResponse order(OrderStatus status, LocalDateTime paidAt, List<OrderItemResponse> items) {
        return new OrderResponse(
                UUID.randomUUID(), null, null, null, null, null,
                status, items, new BigDecimal("10.00"),
                FulfillmentMethod.PICKUP, null, null, null, null, null, null, null,
                PaymentMethod.CASH, new BigDecimal("10.00"), Currency.USD, BigDecimal.ZERO, Currency.USD,
                null, null, null, null,
                null, paidAt, LocalDateTime.now(), LocalDateTime.now(),
                null, null, null, null, null, false, new BigDecimal("10.00"), null, null);
    }
}
