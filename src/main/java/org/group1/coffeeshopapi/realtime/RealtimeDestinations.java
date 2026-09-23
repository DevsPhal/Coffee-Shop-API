package org.group1.coffeeshopapi.realtime;

public final class RealtimeDestinations {
    private RealtimeDestinations() {}

    // Every order change — for the admin/barista dashboards.
    public static final String STAFF_ORDERS = "/topic/orders";

    // A customer's own order changes. Clients subscribe to /user/queue/orders.
    public static final String USER_ORDERS = "/queue/orders";

    // Product, category and extra changes — any signed-in user.
    public static final String CATALOG = "/topic/catalog";

    // "Call staff" presses and answers — staff only.
    public static final String STAFF_CALLS = "/topic/staff-calls";

    // Tells a customer their call was answered. Clients subscribe to /user/queue/staff-calls.
    public static final String USER_STAFF_CALLS = "/queue/staff-calls";

    // Stock level changes — staff only.
    public static final String INVENTORY = "/topic/inventory";

    // New or updated customer feedback — admins only.
    public static final String FEEDBACK = "/topic/feedback";
}
