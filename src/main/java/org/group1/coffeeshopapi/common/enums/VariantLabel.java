package org.group1.coffeeshopapi.common.enums;

public enum VariantLabel {
    MEDIUM("M"), LARGE("L"), PIECE("PC");

    private final String skuSuffix;

    VariantLabel(String skuSuffix) {
        this.skuSuffix = skuSuffix;
    }

    public String getSkuSuffix() {
        return skuSuffix;
    }
}
