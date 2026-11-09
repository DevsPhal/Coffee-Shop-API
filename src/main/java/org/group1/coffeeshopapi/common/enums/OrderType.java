package org.group1.coffeeshopapi.common.enums;

// How a POS (walk-in) order leaves the counter. Online orders don't have one: they use FulfillmentMethod.
public enum OrderType {
    DINE_IN, TAKE_AWAY;

    public FulfillmentMethod fulfillmentMethod() {
        return this == DINE_IN ? FulfillmentMethod.DINE_IN : FulfillmentMethod.PICKUP;
    }
}
