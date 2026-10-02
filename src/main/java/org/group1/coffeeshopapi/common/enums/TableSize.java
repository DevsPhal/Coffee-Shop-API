package org.group1.coffeeshopapi.common.enums;

public enum TableSize {
    SMALL(2), MEDIUM(4), LARGE(6);

    private final int defaultCapacity;

    TableSize(int defaultCapacity) {
        this.defaultCapacity = defaultCapacity;
    }

    public int defaultCapacity() {
        return defaultCapacity;
    }
}
