package org.group1.coffeeshopapi.realtime;

public final class RealtimeDestinations {
    private RealtimeDestinations() {}

    // Every order change — for the admin/barista dashboards.
    public static final String STAFF_ORDERS = "/topic/orders";

    // A customer's own order changes. Clients subscribe to /user/queue/orders.
    public static final String USER_ORDERS = "/queue/orders";
}
