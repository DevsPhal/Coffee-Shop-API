package org.group1.coffeeshopapi.telegram.service.impl;

import org.group1.coffeeshopapi.common.enums.Currency;
import org.group1.coffeeshopapi.common.enums.PaymentMethod;
import org.group1.coffeeshopapi.telegram.dto.OrderInvoice;
import org.group1.coffeeshopapi.telegram.dto.OrderInvoiceLineItem;
import org.group1.coffeeshopapi.telegram.service.TelegramApiClient;
import org.group1.coffeeshopapi.user.entity.Customer;
import org.group1.coffeeshopapi.user.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramInvoiceServiceImplTest {

    private static final Long CHAT_ID = 777L;

    @Mock private CustomerRepository customerRepository;
    @Mock private TelegramApiClient apiClient;
    @InjectMocks private TelegramInvoiceServiceImpl service;

    @Test
    void cashInvoiceShowsTenderedAndChangeBothInUsd() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customerWithChat()));

        OrderInvoice invoice = cashInvoice(new BigDecimal("20.00"), Currency.USD, new BigDecimal("5.50"), Currency.USD);
        service.sendInvoice(customerId, invoice);

        String message = capturedMessage();
        assertThat(message).contains("Tendered (USD): $20.00");
        assertThat(message).contains("Change (USD): $5.50");
    }

    @Test
    void cashInvoiceShowsTenderedAndChangeBothInKhrWhenPaidInKhr() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customerWithChat()));

        OrderInvoice invoice = cashInvoice(new BigDecimal("80000"), Currency.KHR, new BigDecimal("5000"), Currency.KHR);
        service.sendInvoice(customerId, invoice);

        String message = capturedMessage();
        assertThat(message).contains("Tendered (KHR): 80000 KHR");
        assertThat(message).contains("Change (KHR): 5000 KHR");
    }

    @Test
    void cashInvoiceCanShowChangeInADifferentCurrencyThanWhatWasTendered() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customerWithChat()));

        OrderInvoice invoice = cashInvoice(new BigDecimal("80000"), Currency.KHR, new BigDecimal("1.25"), Currency.USD);
        service.sendInvoice(customerId, invoice);

        String message = capturedMessage();
        assertThat(message).contains("Tendered (KHR): 80000 KHR");
        assertThat(message).contains("Change (USD): $1.25");
    }

    @Test
    void bakongInvoiceNeverShowsTenderedOrChangeLines() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customerWithChat()));

        OrderInvoice invoice = new OrderInvoice(UUID.randomUUID(), List.of(lineItem()), null,
                new BigDecimal("14.50"), PaymentMethod.BAKONG, Currency.USD, new BigDecimal("14.50"),
                null, null, null, null, LocalDateTime.now(), null);
        service.sendInvoice(customerId, invoice);

        String message = capturedMessage();
        assertThat(message).doesNotContain("Tendered").doesNotContain("Change").doesNotContain("Total (KHR)");
    }

    @Test
    void rielCashInvoiceShowsTheRoundedRielTotalAndTheRate() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customerWithChat()));

        // $14.50 at 4,100 is 59,450 riel; the last two digits (50) round down to 59,400.
        OrderInvoice invoice = new OrderInvoice(UUID.randomUUID(), List.of(lineItem()), null,
                new BigDecimal("14.50"), PaymentMethod.CASH, null, null, new BigDecimal("60000"), Currency.KHR,
                new BigDecimal("600"), Currency.KHR, LocalDateTime.now(), new BigDecimal("4100.0000"));
        service.sendInvoice(customerId, invoice);

        String message = capturedMessage();
        assertThat(message).contains("Total (KHR): 59400 KHR (1 USD = 4100 KHR)");
        assertThat(message).contains("<b>Total: $14.50</b>");
    }

    @Test
    void rielKhqrInvoiceShowsTheAmountItCollected() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customerWithChat()));

        OrderInvoice invoice = new OrderInvoice(UUID.randomUUID(), List.of(lineItem()), null,
                new BigDecimal("14.50"), PaymentMethod.BAKONG, Currency.KHR, new BigDecimal("59400"),
                null, null, null, null, LocalDateTime.now(), new BigDecimal("4100"));
        service.sendInvoice(customerId, invoice);

        assertThat(capturedMessage()).contains("Total (KHR): 59400 KHR (1 USD = 4100 KHR)");
    }

    @Test
    void doesNothingWhenTheCustomerHasNoLinkedTelegramChat() {
        UUID customerId = UUID.randomUUID();
        Customer unlinked = new Customer();
        unlinked.setId(customerId);
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(unlinked));

        service.sendInvoice(customerId, cashInvoice(BigDecimal.TEN, Currency.USD, BigDecimal.ZERO, Currency.USD));

        verifyNoInteractions(apiClient);
    }

    private OrderInvoice cashInvoice(
            BigDecimal amountTendered, Currency tenderedCurrency, BigDecimal changeDue, Currency changeCurrency) {
        return new OrderInvoice(UUID.randomUUID(), List.of(lineItem()), null, new BigDecimal("14.50"),
                PaymentMethod.CASH, null, null, amountTendered, tenderedCurrency, changeDue, changeCurrency,
                LocalDateTime.now(), null);
    }

    private OrderInvoiceLineItem lineItem() {
        return new OrderInvoiceLineItem("Iced Latte", null, 1, new BigDecimal("14.50"), new BigDecimal("14.50"), List.of());
    }

    private Customer customerWithChat() {
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setTelegramChatId(String.valueOf(CHAT_ID));
        return customer;
    }

    private String capturedMessage() {
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(apiClient).sendHtmlMessage(eq(CHAT_ID), message.capture());
        return message.getValue();
    }
}
