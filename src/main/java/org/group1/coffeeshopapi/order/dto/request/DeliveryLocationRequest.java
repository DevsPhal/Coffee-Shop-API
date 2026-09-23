package org.group1.coffeeshopapi.order.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// A customer's pinned delivery location, plus what the courier needs to find them.
public record DeliveryLocationRequest(
        @NotNull(message = "Latitude is required")
        @DecimalMin(value = "-90", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90", message = "Latitude must be between -90 and 90")
        BigDecimal latitude,

        @NotNull(message = "Longitude is required")
        @DecimalMin(value = "-180", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180", message = "Longitude must be between -180 and 180")
        BigDecimal longitude,

        @NotBlank(message = "A delivery address is required")
        @Size(max = 500) String address,

        @Size(max = 120) String contactName,

        @NotBlank(message = "A contact phone is required for delivery")
        @Size(max = 30) String contactPhone
) {
}
