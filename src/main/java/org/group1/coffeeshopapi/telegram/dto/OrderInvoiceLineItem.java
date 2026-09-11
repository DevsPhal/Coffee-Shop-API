package org.group1.coffeeshopapi.telegram.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderInvoiceLineItem(
        String productName, int quantity, BigDecimal unitPrice, BigDecimal subtotal,
        // Extras (e.g. "Pearl") added to this line — already folded into unitPrice/subtotal, this
        // is just what to display so the customer can see why the price is what it is.
        List<String> extraNames
) {
}
