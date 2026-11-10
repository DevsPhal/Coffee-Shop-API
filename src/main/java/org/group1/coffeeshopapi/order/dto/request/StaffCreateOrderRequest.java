package org.group1.coffeeshopapi.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.group1.coffeeshopapi.common.enums.OrderType;

import java.util.List;

// A POS (walk-in) sale. It is either eaten in or taken away: the POS never takes delivery orders.
public record StaffCreateOrderRequest(
        @NotEmpty(message = "Order must contain at least one item")
        @Valid
        List<OrderItemRequest> items,

        @Size(max = 500, message = "Note must not exceed 500 characters")
        String note,

        @NotNull(message = "Choose dine in or take away")
        OrderType orderType,

        // Optional for dine-in (a counter seat has no table); not allowed for take-away.
        @Size(max = 20, message = "Table number must not exceed 20 characters")
        String tableNumber,

        // The order is for the party already sitting at the table (another round). Without it, an occupied
        // table or one waiting to be cleaned refuses the order, so two parties never share a table by mistake.
        Boolean forSeatedParty,

        // Admin only: seat the order at a reserved table or one waiting to be cleaned. The reason is audited.
        Boolean override,

        @Size(max = 300, message = "The override reason must not exceed 300 characters")
        String overrideReason
) {
    public StaffCreateOrderRequest(List<OrderItemRequest> items, String note, OrderType orderType, String tableNumber) {
        this(items, note, orderType, tableNumber, null, null, null);
    }
}
