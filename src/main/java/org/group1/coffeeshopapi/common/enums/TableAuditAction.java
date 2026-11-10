package org.group1.coffeeshopapi.common.enums;

public enum TableAuditAction {
    // Staff changed the status by hand (seat guests, guests left, marked clean...).
    STATUS_CHANGED,
    // An order seated a new party at a free table.
    ORDER_SEATED,
    // An order was added for the party already at the table.
    ORDER_ADDED,
    // The table's last active order finished, so the table now needs cleaning.
    ORDERS_FINISHED,
    // A booking that starts soon now holds the table.
    RESERVATION_HELD,
    // The booked party arrived and was seated.
    RESERVATION_CHECKED_IN,
    // A booking was cancelled, moved or missed, so the table is free again.
    RESERVATION_RELEASED
}
