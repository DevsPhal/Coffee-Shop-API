package org.group1.coffeeshopapi.telegram.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderInvoiceLineItem(
        String productName, int quantity, BigDecimal unitPrice, BigDecimal subtotal,
        // Extra names to display (e.g. "Pearl") — already included in unitPrice/subtotal.
        List<String> extraNames
) {
}
