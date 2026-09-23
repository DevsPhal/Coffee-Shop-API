package org.group1.coffeeshopapi.order.service;

import org.group1.coffeeshopapi.order.dto.response.StaffCallResponse;

import java.util.List;
import java.util.UUID;

public interface StaffCallService {

    // Customer asks for help with one of their active orders. At most once per cooldown window.
    StaffCallResponse call(UUID orderId, UUID customerId);

    // Calls no staff member has answered yet, oldest first — for a dashboard that just loaded.
    List<StaffCallResponse> listOpen();

    // Staff member takes the call; clears the alert everywhere and tells the customer.
    void answer(UUID orderId, UUID actorId);
}
