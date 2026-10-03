package org.group1.coffeeshopapi.table.dto.response;

import org.group1.coffeeshopapi.order.dto.response.OrderResponse;
import org.group1.coffeeshopapi.order.dto.response.StaffCallResponse;

import java.util.List;

public record TableActivityResponse(
        TableResponse table,
        int activeOrderCount,
        int openStaffCallCount,
        List<OrderResponse> activeOrders,
        List<StaffCallResponse> openStaffCalls
) {
}
