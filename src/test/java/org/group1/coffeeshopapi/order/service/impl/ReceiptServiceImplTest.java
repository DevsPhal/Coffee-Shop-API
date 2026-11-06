package org.group1.coffeeshopapi.order.service.impl;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
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
        UUID orderId = UUID.randomUUID();
        when(orderService.getAny(orderId))
                .thenReturn(paidOrder("សុខ ដារ៉ា 🙂", "josé", List.of(latte()), null, "Ring twice — thanks"));

        String text = text(service.generateInvoicePdf(orderId, ReceiptLanguage.EN));

        assertThat(text).contains("Customer:", "Served By: Barista José", "Note: Ring twice — thanks")
                .doesNotContain("?");
    }

    @Test
    void englishAndKhmerInvoicesPrintTheSameOrderNumberAndAmounts() throws IOException {
        UUID orderId = UUID.randomUUID();
        when(orderService.getAny(orderId))
                .thenReturn(paidOrder("Dara", "Sokha", List.of(latte(), latte()), new BigDecimal("1.00"), null));

        String english = text(service.generateInvoicePdf(orderId, ReceiptLanguage.EN));
        String khmer = text(service.generateInvoicePdf(orderId, ReceiptLanguage.KH));

        assertThat(figures(khmer)).isEqualTo(figures(english))
                .contains("$5.00", "$1.00", "$10.00", "40000 KHR", "2000 KHR");
        assertThat(english).contains("Subtotal", "Delivery Fee", "TOTAL", "Payment Method: Cash", "Thank you, come again!");
        assertThat(khmer).doesNotContain("Subtotal", "TOTAL", "Payment Method", "Customer", "Thank you");
    }

    @Test
    void longNamesAndNotesWrapInsideThePaperMarginsInBothLanguages() throws IOException {
        UUID orderId = UUID.randomUUID();
        String longKhmer = "កាហ្វេទឹកដោះគោទឹកកក".repeat(4);
        OrderItemResponse item = new OrderItemResponse(
                UUID.randomUUID(), UUID.randomUUID(), "Caramel Macchiato With Extra Whipped Cream And Hazelnut Syrup",
                longKhmer, 12, new BigDecimal("3.90"), new BigDecimal("46.80"),
                VariantLabel.LARGE, SugarLevel.LESS, null, null, List.of(), null, null);
        when(orderService.getAny(orderId)).thenReturn(paidOrder(
                "Customer With A Remarkably Long Telegram Display Name", "Sokha", List.of(item), null,
                "Please leave the order with the security guard at the front gate.\n" + longKhmer));

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
        return Pattern.compile("#[0-9A-F]{8}|\\$\\d+\\.\\d{2}|\\d+ KHR").matcher(text).results()
                .map(match -> match.group()).toList();
    }

    private static OrderItemResponse latte() {
        return new OrderItemResponse(
                UUID.randomUUID(), UUID.randomUUID(), "Iced Latte", "ឡាតេទឹកកក",
                2, new BigDecimal("2.50"), new BigDecimal("5.00"),
                VariantLabel.LARGE, SugarLevel.LESS, null, null, List.of(), null, null);
    }

    private OrderResponse paidOrder(String customerName, String staffName, List<OrderItemResponse> items,
                                    BigDecimal deliveryFee, String note) {
        LocalDateTime paidAt = LocalDateTime.of(2026, 11, 7, 15, 45);
        return new OrderResponse(
                UUID.randomUUID(), null, staffName, Role.BARISTA, null, customerName,
                OrderStatus.COMPLETED, items, new BigDecimal("10.00"),
                FulfillmentMethod.PICKUP, null, null, null, null, null, null, null,
                PaymentMethod.CASH, new BigDecimal("40000"), Currency.KHR, new BigDecimal("2000"), Currency.KHR,
                null, null, null, null,
                note, paidAt, paidAt, paidAt,
                null, null, deliveryFee, null, null, false, new BigDecimal("10.00"), null, null);
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
