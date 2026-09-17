package org.group1.coffeeshopapi.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

// A walk-in POS sale rung up by an admin or barista. Always pickup — no delivery option, since
// the customer is standing right there.
public record StaffCreateOrderRequest(
        @NotEmpty(message = "Order must contain at least one item")
        @Valid
        List<OrderItemRequest> items,

        @Size(max = 500, message = "Note must not exceed 500 characters")
        String note
) {
}
