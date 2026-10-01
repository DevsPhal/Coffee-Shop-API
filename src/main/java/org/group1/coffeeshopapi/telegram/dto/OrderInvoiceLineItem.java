package org.group1.coffeeshopapi.telegram.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderInvoiceLineItem(
        String productName, String productNameKh, int quantity, BigDecimal unitPrice, BigDecimal subtotal,
        List<String> extraNames
) {
}
