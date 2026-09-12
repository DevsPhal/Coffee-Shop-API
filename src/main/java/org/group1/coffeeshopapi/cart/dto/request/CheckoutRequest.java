package org.group1.coffeeshopapi.cart.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

// deliveryLatitude/deliveryLongitude are optional and must both be given together (see
// OrderServiceImpl.createForCustomer) — omit both for a pickup order, the shop's default.
public record CheckoutRequest(
        String note,

        @DecimalMin(value = "-90", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90", message = "Latitude must be between -90 and 90")
        BigDecimal deliveryLatitude,

        @DecimalMin(value = "-180", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180", message = "Longitude must be between -180 and 180")
        BigDecimal deliveryLongitude
) {
}
