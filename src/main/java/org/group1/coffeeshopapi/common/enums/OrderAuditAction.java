package org.group1.coffeeshopapi.common.enums;

public enum OrderAuditAction {
    CREATED, CASH_COLLECTED, BAKONG_CONFIRMED, CANCELLED, DELIVERY_FEE_SET,
    // Customer switched to cash, or a Bakong QR was (re)generated.
    CASH_SELECTED, BAKONG_QR_GENERATED,
    // The rest of the order lifecycle after payment, named to match the status it moved into.
    PREPARING, OUT_FOR_DELIVERY, DELIVERED, COMPLETED
}
