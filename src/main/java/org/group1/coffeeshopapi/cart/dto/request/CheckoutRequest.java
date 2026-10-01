package org.group1.coffeeshopapi.cart.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import org.group1.coffeeshopapi.order.dto.request.CheckoutDetailsRequest;

import java.math.BigDecimal;

public record CheckoutRequest(
        @Size(max = 500, message = "Note must not exceed 500 characters")
        String note,

        @DecimalMin(value = "-90", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90", message = "Latitude must be between -90 and 90")
        BigDecimal deliveryLatitude,

        @DecimalMin(value = "-180", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180", message = "Longitude must be between -180 and 180")
        BigDecimal deliveryLongitude,

        @Valid CheckoutDetailsRequest delivery
) {
}
