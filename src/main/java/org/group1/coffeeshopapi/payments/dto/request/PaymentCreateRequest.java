package org.group1.coffeeshopapi.payments.dto.request;

import java.util.UUID;

import org.group1.coffeeshopapi.common.enums.PaymentMethod;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentCreateRequest {

    @NotNull(message = "Order ID is required")
    private UUID orderId;

    @NotNull(message = "Payment method is required")
    private PaymentMethod method;

    /** ISO 4217 currency code. Defaults to USD when omitted. Only USD and KHR are supported by Bakong KHQR. */
    private String currency;
}
