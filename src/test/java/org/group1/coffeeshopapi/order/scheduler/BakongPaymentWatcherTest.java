package org.group1.coffeeshopapi.order.scheduler;

import org.group1.coffeeshopapi.common.exception.PaymentVerificationUnavailableException;
import org.group1.coffeeshopapi.common.properties.BakongProperties;
import org.group1.coffeeshopapi.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BakongPaymentWatcherTest {

    @Mock private OrderService orderService;
    @Mock private BakongProperties bakongProperties;
    @InjectMocks private BakongPaymentWatcher watcher;

    private final UUID first = UUID.randomUUID();
    private final UUID second = UUID.randomUUID();

    @Test
    void checksEveryOrderAwaitingBakongPayment() {
        when(bakongProperties.isConfigured()).thenReturn(true);
        when(orderService.listOrdersAwaitingBakongPayment()).thenReturn(List.of(first, second));

        watcher.confirmPaidOrders();

        verify(orderService).confirmBakongPaymentAutomatically(first);
        verify(orderService).confirmBakongPaymentAutomatically(second);
    }

    @Test
    void oneFailingOrderDoesNotBlockTheRest() {
        when(bakongProperties.isConfigured()).thenReturn(true);
        when(orderService.listOrdersAwaitingBakongPayment()).thenReturn(List.of(first, second));
        when(orderService.confirmBakongPaymentAutomatically(first)).thenThrow(new IllegalStateException("boom"));

        watcher.confirmPaidOrders();

        verify(orderService).confirmBakongPaymentAutomatically(second);
    }

    @Test
    void waitsForTheNextRunWhenBakongIsUnreachable() {
        when(bakongProperties.isConfigured()).thenReturn(true);
        when(orderService.listOrdersAwaitingBakongPayment()).thenReturn(List.of(first, second));
        when(orderService.confirmBakongPaymentAutomatically(first))
                .thenThrow(new PaymentVerificationUnavailableException("down"));

        watcher.confirmPaidOrders();

        verify(orderService, never()).confirmBakongPaymentAutomatically(second);
    }

    @Test
    void doesNothingWhenBakongIsNotConfigured() {
        when(bakongProperties.isConfigured()).thenReturn(false);

        watcher.confirmPaidOrders();

        verifyNoInteractions(orderService);
    }
}
