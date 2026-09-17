package org.group1.coffeeshopapi.common.enums;

/**
 * Order lifecycle, in order:
 * PENDING (placed) → PAID (money in) → PREPARING (being made) → OUT_FOR_DELIVERY (delivery only)
 * → COMPLETED (pickup) or DELIVERED (delivery). CANCELLED only happens from PENDING.
 * <p>
 * A cash order can skip straight from PENDING to PREPARING and get paid later, at handover — so
 * paidAt, not this status, is what actually means "this order is paid."
 */
public enum OrderStatus {
    PENDING, PAID, PREPARING, OUT_FOR_DELIVERY, COMPLETED, DELIVERED, CANCELLED;

    // Terminal — an order here can't move any further.
    public boolean isFinished() {
        return this == COMPLETED || this == DELIVERED || this == CANCELLED;
    }
}
