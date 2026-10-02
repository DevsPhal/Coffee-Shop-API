package org.group1.coffeeshopapi.order.service;

import org.group1.coffeeshopapi.order.dto.request.StaffCallRequest;
import org.group1.coffeeshopapi.order.dto.response.StaffCallResponse;

import java.util.List;
import java.util.UUID;

public interface StaffCallService {

    StaffCallResponse call(UUID orderId, UUID customerId, StaffCallRequest request);

    List<StaffCallResponse> listOpen();

    void answer(UUID orderId, UUID actorId);
}
