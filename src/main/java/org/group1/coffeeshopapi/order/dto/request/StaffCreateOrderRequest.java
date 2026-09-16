package org.group1.coffeeshopapi.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

// A walk-in POS sale rung up by an admin or barista — the customer is standing at the counter, so
// this is always served on the spot as a pickup order. Deliberately has no delivery option (unlike
// a customer's own app checkout — see CheckoutRequest): there's no GPS pin and no courier for a
// sale rung up in person, so offering "deliver this" here would be a fulfillment path the rest of
// the system (dispatch, delivery board) has no real information to act on. See
// OrderServiceImpl#create.
public record StaffCreateOrderRequest(
        @NotEmpty(message = "Order must contain at least one item")
        @Valid
        List<OrderItemRequest> items,

        @Size(max = 500, message = "Note must not exceed 500 characters")
        String note
) {
}
