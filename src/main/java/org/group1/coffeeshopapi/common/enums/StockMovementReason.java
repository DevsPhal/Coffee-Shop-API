package org.group1.coffeeshopapi.common.enums;

public enum StockMovementReason {
    PURCHASE,
    SALE,
    // Sold items brought back and put back on the shelf.
    RETURN,
    WASTE,
    // Manual stock-in/stock-cut and stock counts.
    ADJUSTMENT
}
