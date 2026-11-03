package org.group1.coffeeshopapi.common.enums;

// Which unit a stock-in or stock-cut quantity is given in. Stock itself is always kept in base units.
public enum QuantityUnit {
    // The product's stock unit, the one it is bought in (e.g. CARTON); converted with unitsPerStock.
    PURCHASE,
    // The product's sell unit, the one stock is counted in (e.g. CAN).
    BASE
}
