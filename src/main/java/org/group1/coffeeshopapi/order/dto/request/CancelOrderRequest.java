package org.group1.coffeeshopapi.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Cancelling an order always says why. restock is the staff's call for an order whose stock was already taken:
// true (the default) puts it back on the shelf because nothing was made, false writes it off as waste.
// A customer's cancellation always puts the stock back.
public record CancelOrderRequest(
        @NotBlank(message = "Give a reason for cancelling")
        @Size(max = 300, message = "The reason must not exceed 300 characters")
        String reason,
        Boolean restock
) {
    public CancelOrderRequest(String reason) {
        this(reason, null);
    }

    public boolean backToStock() {
        return !Boolean.FALSE.equals(restock);
    }
}
