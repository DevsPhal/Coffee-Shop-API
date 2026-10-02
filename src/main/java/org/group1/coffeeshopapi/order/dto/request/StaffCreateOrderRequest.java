package org.group1.coffeeshopapi.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record StaffCreateOrderRequest(
        @NotEmpty(message = "Order must contain at least one item")
        @Valid
        List<OrderItemRequest> items,

        @Size(max = 500, message = "Note must not exceed 500 characters")
        String note,

        @Size(max = 20, message = "Table number must not exceed 20 characters")
        String tableNumber
) {
    public StaffCreateOrderRequest(List<OrderItemRequest> items, String note) {
        this(items, note, null);
    }
}
