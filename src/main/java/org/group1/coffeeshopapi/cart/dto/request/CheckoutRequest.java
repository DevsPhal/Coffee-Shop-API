package org.group1.coffeeshopapi.cart.dto.request;

import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import org.group1.coffeeshopapi.order.dto.request.CheckoutDetailsRequest;
import org.group1.coffeeshopapi.common.enums.PaymentMethod;

public record CheckoutRequest(
        @Size(max = 500, message = "Note must not exceed 500 characters")
        String note,
        @Valid CheckoutDetailsRequest delivery,
        PaymentMethod paymentMethod
) {
    public CheckoutRequest(String note) {
        this(note, null, null);
    }
}
