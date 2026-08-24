package org.group1.coffeeshopapi.telegram.dto;

import java.math.BigDecimal;

public record OrderInvoiceLineItem(String productName, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {
}
