package org.group1.coffeeshopapi.realtime;

import java.util.List;

public enum ResourceType {
    PRODUCT(RealtimeDestinations.CATALOG),
    CATEGORY(RealtimeDestinations.CATALOG),
    EXTRA(RealtimeDestinations.CATALOG),
    BANNER(RealtimeDestinations.CATALOG),
    EVENT(RealtimeDestinations.CATALOG),
    INVENTORY(RealtimeDestinations.INVENTORY, RealtimeDestinations.STOCK),
    FEEDBACK(RealtimeDestinations.FEEDBACK),
    TABLE(RealtimeDestinations.TABLES),
    // Bookings change what the tables screen shows; the message only carries the id.
    RESERVATION(RealtimeDestinations.TABLES);

    private final List<String> destinations;

    ResourceType(String... destinations) {
        this.destinations = List.of(destinations);
    }

    public List<String> destinations() {
        return destinations;
    }
}
