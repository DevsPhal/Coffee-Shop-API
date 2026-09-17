package org.group1.coffeeshopapi.common.enums;

public enum OrderAuditAction {
    CREATED, CASH_COLLECTED, BAKONG_CONFIRMED, CANCELLED, DELIVERY_FEE_SET,
    // The rest of the order lifecycle after payment, named to match the status it moved into.
    PREPARING, OUT_FOR_DELIVERY, DELIVERED, COMPLETED
}
