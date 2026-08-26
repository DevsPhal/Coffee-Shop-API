package org.group1.coffeeshopapi.telegram.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.enums.Currency;
import org.group1.coffeeshopapi.common.enums.PaymentMethod;
import org.group1.coffeeshopapi.telegram.dto.OrderInvoice;
import org.group1.coffeeshopapi.telegram.dto.OrderInvoiceLineItem;
import org.group1.coffeeshopapi.telegram.service.TelegramApiClient;
import org.group1.coffeeshopapi.telegram.service.TelegramInvoiceService;
import org.group1.coffeeshopapi.telegram.util.TelegramFormat;
import org.group1.coffeeshopapi.user.entity.Customer;
import org.group1.coffeeshopapi.user.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TelegramInvoiceServiceImpl implements TelegramInvoiceService {

    private static final DateTimeFormatter PAID_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CustomerRepository customerRepository;
    private final TelegramApiClient apiClient;

    @Override
    public void sendInvoice(UUID customerId, OrderInvoice invoice) {
        if (customerId == null) {
            return;
        }
        Customer customer = customerRepository.findById(customerId).orElse(null);
        if (customer == null || customer.getTelegramChatId() == null) {
            return;
        }

        apiClient.sendHtmlMessage(Long.parseLong(customer.getTelegramChatId()), buildInvoiceMessage(invoice));
    }

    private String buildInvoiceMessage(OrderInvoice invoice) {
        String billNumber = "ORD-" + invoice.orderId().toString().substring(0, 8).toUpperCase();

        StringBuilder sb = new StringBuilder("🧾 <b>Invoice — ")
                .append(billNumber)
                .append("</b>\n\n");

        for (OrderInvoiceLineItem item : invoice.items()) {
            sb.append("• ").append(TelegramFormat.escape(TelegramFormat.titleCase(item.productName())))
                    .append(" × ").append(item.quantity())
                    .append(" — ").append(TelegramFormat.usd(item.subtotal()))
                    .append('\n');
        }

        sb.append("\n<b>Total: ").append(TelegramFormat.usd(invoice.totalAmount())).append("</b>\n");
        sb.append("Payment: ").append(paymentSummary(invoice)).append('\n');
        if (invoice.paidAt() != null) {
            sb.append("Paid at: ").append(invoice.paidAt().format(PAID_AT_FORMAT)).append('\n');
        }
        sb.append("\nThank you for your order! ☕");
        return sb.toString();
    }

    private String paymentSummary(OrderInvoice invoice) {
        if (invoice.paymentMethod() != PaymentMethod.BAKONG) {
            return "Cash";
        }
        boolean convertedCurrency = invoice.bakongCurrency() != null && invoice.bakongAmount() != null
                && invoice.bakongCurrency() != Currency.USD;
        return convertedCurrency
                ? "Bakong KHQR (" + TelegramFormat.wholeAmount(invoice.bakongAmount(), invoice.bakongCurrency().name()) + ")"
                : "Bakong KHQR";
    }
}
