package org.group1.coffeeshopapi.table.service.impl;

import org.group1.coffeeshopapi.common.enums.FulfillmentMethod;
import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.enums.StaffCallReason;
import org.group1.coffeeshopapi.common.enums.StaffCallStatus;
import org.group1.coffeeshopapi.common.enums.TableSize;
import org.group1.coffeeshopapi.common.enums.TableStatus;
import org.group1.coffeeshopapi.order.dto.response.OrderResponse;
import org.group1.coffeeshopapi.order.dto.response.StaffCallResponse;
import org.group1.coffeeshopapi.order.service.OrderService;
import org.group1.coffeeshopapi.order.service.StaffCallService;
import org.group1.coffeeshopapi.table.dto.response.TableActivityResponse;
import org.group1.coffeeshopapi.table.dto.response.TableResponse;
import org.group1.coffeeshopapi.table.service.DiningTableService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TableActivityServiceImplTest {

    @Mock private DiningTableService diningTableService;
    @Mock private OrderService orderService;
    @Mock private StaffCallService staffCallService;
    @InjectMocks private TableActivityServiceImpl service;

    @Test
    void groupsActiveOrdersAndOpenCallsUnderTheirTable() {
        TableResponse busy = table("A1", TableStatus.OCCUPIED);
        TableResponse free = table("A2", TableStatus.AVAILABLE);
        OrderResponse order = order(busy.id());
        OrderResponse takeaway = order(null);
        when(diningTableService.listPublic(null)).thenReturn(List.of(busy, free));
        when(orderService.listActiveDineIn()).thenReturn(List.of(order));
        when(staffCallService.listOpen()).thenReturn(List.of(call(order.id()), call(takeaway.id())));

        List<TableActivityResponse> activity = service.list(null);

        assertThat(activity.get(0).activeOrders()).containsExactly(order);
        assertThat(activity.get(0).openStaffCallCount()).isEqualTo(1);
        assertThat(activity.get(1).activeOrderCount()).isZero();
        assertThat(activity.get(1).openStaffCalls()).isEmpty();
    }

    private TableResponse table(String number, TableStatus status) {
        return new TableResponse(UUID.randomUUID(), number, TableSize.MEDIUM, 4, 0, status,
                "https://590stcafe.shop/table/" + number, LocalDateTime.now(), LocalDateTime.now());
    }

    private OrderResponse order(UUID tableId) {
        return new OrderResponse(
                UUID.randomUUID(), null, null, null, null, "Dara",
                OrderStatus.PREPARING, List.of(), BigDecimal.TEN,
                FulfillmentMethod.DINE_IN, tableId, "A1", null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null,
                null, null, LocalDateTime.now(), LocalDateTime.now(),
                null, null, null, null, null, false, BigDecimal.TEN, null);
    }

    private StaffCallResponse call(UUID orderId) {
        return new StaffCallResponse(orderId, "Dara", OrderStatus.PREPARING, FulfillmentMethod.DINE_IN, "A1",
                StaffCallStatus.OPEN, StaffCallReason.OTHER, "More napkins", LocalDateTime.now(),
                null, null, null, null);
    }
}
