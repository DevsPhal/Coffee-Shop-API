package org.group1.coffeeshopapi.common.enums;

/**
 * The life of an order, in the order it happens:
 * <ul>
 *   <li>{@link #PENDING} — placed but not paid for. The only status an order can be
 *   {@link #CANCELLED} from, since nothing has been charged and no stock has moved yet.</li>
 *   <li>{@link #PAID} — payment confirmed (cash collected, or a Bakong transfer verified against
 *   the bank). Stock is cut and {@code paidAt} is stamped at this point, so <em>this</em> is the
 *   status the money side of the app keys off — not {@link #COMPLETED}.</li>
 *   <li>{@link #PREPARING} — a barista has picked the order up and started making it.</li>
 *   <li>{@link #OUT_FOR_DELIVERY} — a delivery order has left the shop with a courier.
 *   Delivery orders only.</li>
 *   <li>{@link #COMPLETED} — a pickup order handed to the customer at the counter. Terminal,
 *   and only reachable by a pickup order.</li>
 *   <li>{@link #DELIVERED} — a delivery order confirmed as arrived. Terminal, and the delivery
 *   counterpart of {@link #COMPLETED}.</li>
 *   <li>{@link #CANCELLED} — called off before payment. Terminal.</li>
 * </ul>
 * PAID and PREPARING only ever move forward, and neither can be cancelled: once money has
 * changed hands, unwinding it is a refund, which this app does not model.
 */
public enum OrderStatus {
    PENDING, PAID, PREPARING, OUT_FOR_DELIVERY, COMPLETED, DELIVERED, CANCELLED;

    /** Terminal states — an order here can no longer move. */
    public boolean isFinished() {
        return this == COMPLETED || this == DELIVERED || this == CANCELLED;
    }
}
