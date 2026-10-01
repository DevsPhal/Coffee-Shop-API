package org.group1.coffeeshopapi.realtime;

public final class RealtimeDestinations {
    private RealtimeDestinations() {}

    public static final String STAFF_ORDERS = "/topic/orders";

    public static final String USER_ORDERS = "/queue/orders";

    public static final String CATALOG = "/topic/catalog";

    public static final String STAFF_CALLS = "/topic/staff-calls";

    public static final String USER_STAFF_CALLS = "/queue/staff-calls";

    public static final String INVENTORY = "/topic/inventory";

    public static final String FEEDBACK = "/topic/feedback";
}
