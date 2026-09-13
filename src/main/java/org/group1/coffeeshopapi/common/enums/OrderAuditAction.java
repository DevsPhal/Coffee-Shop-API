package org.group1.coffeeshopapi.common.enums;

public enum OrderAuditAction {
    CREATED, CASH_COLLECTED, BAKONG_CONFIRMED, CANCELLED, DELIVERY_FEE_SET,
    // The rest of the lifecycle after payment — see OrderStatus for what each one means and the
    // order they happen in. Named to match the OrderStatus value the order moved into.
    PREPARING, OUT_FOR_DELIVERY, DELIVERED, COMPLETED
}
