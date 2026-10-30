package org.group1.coffeeshopapi.order.dto.request;

import org.group1.coffeeshopapi.common.enums.FulfillmentMethod;
import org.group1.coffeeshopapi.common.enums.OrderChannel;
import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.enums.PaymentMethod;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Optional filters for staff order lists; a null (or empty) value doesn't filter.
 * {@code search} matches the order number ("#A1B2C3D4", or its first characters), the customer's name or the
 * contact name. {@code from} and {@code to} are shop-time days, both included, on when the order was placed.
 */
public record OrderSearchFilter(
        UUID baristaId,
        UUID customerId,
        List<OrderStatus> statuses,
        String search,
        LocalDate from,
        LocalDate to,
        OrderChannel channel,
        FulfillmentMethod fulfillment,
        PaymentMethod paymentMethod
) {
}
