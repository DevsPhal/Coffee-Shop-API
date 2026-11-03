package org.group1.coffeeshopapi.common.enums;

public enum VariantLabel {
    MEDIUM("M"), LARGE("L"), PIECE("PC"),
    // Sell several base units at once, e.g. a whole CARTON of 24 cans.
    PACK("PK"), BOX("BX"), CARTON("CT");

    private final String skuSuffix;

    VariantLabel(String skuSuffix) {
        this.skuSuffix = skuSuffix;
    }

    public String getSkuSuffix() {
        return skuSuffix;
    }

    public boolean isMultiUnit() {
        return this == PACK || this == BOX || this == CARTON;
    }
}
