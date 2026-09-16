package org.group1.coffeeshopapi.extra.dto.request;

import jakarta.validation.constraints.DecimalMin;
import org.group1.coffeeshopapi.common.enums.Status;

import java.math.BigDecimal;

// quantityOnHand null means "leave unchanged" — same convention as every other optional field
// here. This is also how an admin restocks an extra (PATCH a new number in) — see
// Extra.quantityOnHand for why there's no dedicated stock-in/stock-cut endpoint like Product has.
public record UpdateExtraRequest(
        String name,

        @DecimalMin(value = "0.0", message = "Price must not be negative")
        BigDecimal price,

        Status status,

        @DecimalMin(value = "0.0", message = "Quantity on hand must not be negative")
        BigDecimal quantityOnHand
) {
}
