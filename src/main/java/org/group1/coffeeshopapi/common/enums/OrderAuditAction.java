package org.group1.coffeeshopapi.common.enums;

/**
 * One row per thing a person did to an order. Each constant is named after the state it leaves
 * the order in where there is one ({@link OrderStatus#PREPARING}, {@link OrderStatus#COMPLETED},
 * {@link OrderStatus#CANCELLED}), since {@code Order.handledBy} only ever holds the most recent
 * actor and this is the only record of who did what and when.
 */
public enum OrderAuditAction {
    CREATED, CASH_COLLECTED, BAKONG_CONFIRMED, PREPARING_STARTED, DISPATCHED, DELIVERED,
    COMPLETED, CANCELLED
}
