package org.group1.coffeeshopapi.order.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.group1.coffeeshopapi.common.enums.FulfillmentMethod;

public record CheckoutDetailsRequest(
        @NotNull FulfillmentMethod method,
        @Size(max = 120) String contactName,
        @Size(max = 30) String contactPhone,
        @Size(max = 500) String address
) {}
