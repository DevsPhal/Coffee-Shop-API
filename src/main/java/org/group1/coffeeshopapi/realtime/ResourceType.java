package org.group1.coffeeshopapi.realtime;

public enum ResourceType {
    PRODUCT(RealtimeDestinations.CATALOG),
    CATEGORY(RealtimeDestinations.CATALOG),
    EXTRA(RealtimeDestinations.CATALOG),
    INVENTORY(RealtimeDestinations.INVENTORY),
    FEEDBACK(RealtimeDestinations.FEEDBACK),
    TABLE(RealtimeDestinations.TABLES);

    private final String destination;

    ResourceType(String destination) {
        this.destination = destination;
    }

    public String destination() {
        return destination;
    }
}
