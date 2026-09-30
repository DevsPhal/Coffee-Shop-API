package org.group1.coffeeshopapi.common.enums;

public enum OrderAuditAction {
    CREATED, CASH_COLLECTED, BAKONG_CONFIRMED, CANCELLED, DELIVERY_FEE_SET,
    // Customer switched to cash, or a Bakong QR was (re)generated.
    CASH_SELECTED, BAKONG_QR_GENERATED,
    // Customer pinned or moved their delivery location — staff has to quote the fee again.
    LOCATION_PINNED,
    // Customer pressed "call staff", and a staff member answered it.
    STAFF_CALLED, STAFF_CALL_ANSWERED,
    // The rest of the order lifecycle after payment, named to match the status it moved into.
    PREPARING, OUT_FOR_DELIVERY, DELIVERED, COMPLETED
}
