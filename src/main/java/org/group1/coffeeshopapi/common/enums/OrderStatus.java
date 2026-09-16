package org.group1.coffeeshopapi.common.enums;

/**
 * The life of an order, in the order it happens:
 * <ul>
 *   <li>{@link #PENDING} — placed, and either not yet paid or (for cash) not yet claimed by
 *   staff. The only status an order can be {@link #CANCELLED} from, since a Bakong order here
 *   hasn't been charged, and a cash order here — even if a barista has already started making it
 *   under the real-world "make it, collect on handover" flow — still has an easy out: nothing has
 *   actually been handed over yet.</li>
 *   <li>{@link #PAID} — payment confirmed up front (cash collected before prep started, or a
 *   Bakong transfer verified against the bank). Stock is cut and {@code paidAt} is stamped at this
 *   point. A cash order can instead skip straight from {@link #PENDING} to {@link #PREPARING}
 *   without ever passing through here — see {@code OrderServiceImpl#startPreparing}/{@code
 *   #markPaid} — so {@code paidAt}, not this enum, is the one signal the money side of the app
 *   (reporting, "can this be handed over") actually keys off.</li>
 *   <li>{@link #PREPARING} — a barista has picked the order up and started making it. For a
 *   Bakong order this always means paid; for a cash order it does not necessarily — the cash may
 *   still be collected later, at handover.</li>
 *   <li>{@link #OUT_FOR_DELIVERY} — a delivery order has left the shop with a courier.
 *   Delivery orders only. A cash delivery order can be dispatched with its cash still uncollected;
 *   it's collected from the customer on arrival.</li>
 *   <li>{@link #COMPLETED} — a pickup order handed to the customer at the counter. Terminal,
 *   only reachable by a pickup order, and only once {@code paidAt} is set.</li>
 *   <li>{@link #DELIVERED} — a delivery order confirmed as arrived. Terminal, the delivery
 *   counterpart of {@link #COMPLETED}, and likewise gated on {@code paidAt} being set.</li>
 *   <li>{@link #CANCELLED} — called off while still {@link #PENDING}. Terminal.</li>
 * </ul>
 * PAID and PREPARING only ever move forward, and neither can be cancelled through this app's
 * normal cancel endpoints — for a Bakong order that's because money has already changed hands and
 * unwinding it is a refund this app does not model; for a cash order already in PREPARING it's a
 * deliberate business call: once the shop has committed to making it, calling it off is a
 * conversation with the customer at the counter/door, not a self-service button.
 */
public enum OrderStatus {
    PENDING, PAID, PREPARING, OUT_FOR_DELIVERY, COMPLETED, DELIVERED, CANCELLED;

    /** Terminal states — an order here can no longer move. */
    public boolean isFinished() {
        return this == COMPLETED || this == DELIVERED || this == CANCELLED;
    }
}
