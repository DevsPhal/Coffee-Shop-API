package org.group1.coffeeshopapi.cart.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.group1.coffeeshopapi.order.dto.request.CheckoutDetailsRequest;

import java.math.BigDecimal;

// deliveryLatitude/deliveryLongitude must be given together, or not at all (pickup, the default).
// A delivery order needs the pin plus delivery details with an address and contact phone; staff
// then quote the fee from the pin's distance before the order can be paid.
public record CheckoutRequest(
        String note,

        @DecimalMin(value = "-90", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90", message = "Latitude must be between -90 and 90")
        BigDecimal deliveryLatitude,

        @DecimalMin(value = "-180", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180", message = "Longitude must be between -180 and 180")
        BigDecimal deliveryLongitude,

        // Fulfillment method, plus the address/contact a courier needs for DELIVERY. Omit for
        // pickup, the default.
        @Valid CheckoutDetailsRequest delivery
) {
}
