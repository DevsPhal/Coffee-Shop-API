package org.group1.coffeeshopapi.realtime;

// A kind of data clients can watch, and the topic its changes go to.
public enum ResourceType {
    PRODUCT(RealtimeDestinations.CATALOG),
    CATEGORY(RealtimeDestinations.CATALOG),
    EXTRA(RealtimeDestinations.CATALOG),
    // id is the product id, since stock is tracked per product.
    INVENTORY(RealtimeDestinations.INVENTORY),
    FEEDBACK(RealtimeDestinations.FEEDBACK);

    private final String destination;

    ResourceType(String destination) {
        this.destination = destination;
    }

    public String destination() {
        return destination;
    }
}
