package org.group1.coffeeshopapi.common.enums;

public enum OrderStatus {
    PENDING, PAID, PREPARING, OUT_FOR_DELIVERY, COMPLETED, DELIVERED, CANCELLED;

    public boolean isFinished() {
        return this == COMPLETED || this == DELIVERED || this == CANCELLED;
    }
}
