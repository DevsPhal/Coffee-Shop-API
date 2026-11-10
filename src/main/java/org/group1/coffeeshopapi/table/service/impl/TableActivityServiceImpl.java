package org.group1.coffeeshopapi.table.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.enums.TableStatus;
import org.group1.coffeeshopapi.order.dto.response.OrderResponse;
import org.group1.coffeeshopapi.order.dto.response.StaffCallResponse;
import org.group1.coffeeshopapi.order.service.OrderService;
import org.group1.coffeeshopapi.order.service.StaffCallService;
import org.group1.coffeeshopapi.table.dto.response.ReservationResponse;
import org.group1.coffeeshopapi.table.dto.response.TableActivityResponse;
import org.group1.coffeeshopapi.table.dto.response.TableResponse;
import org.group1.coffeeshopapi.table.service.DiningTableService;
import org.group1.coffeeshopapi.table.service.ReservationService;
import org.group1.coffeeshopapi.table.service.TableActivityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TableActivityServiceImpl implements TableActivityService {

    private final DiningTableService diningTableService;
    private final OrderService orderService;
    private final StaffCallService staffCallService;
    private final ReservationService reservationService;

    @Override
    public List<TableActivityResponse> list(TableStatus status) {
        List<TableResponse> tables = diningTableService.listPublic(status);
        Map<UUID, List<OrderResponse>> ordersByTable = activeOrdersByTable();
        List<StaffCallResponse> openCalls = staffCallService.listOpen();
        Map<UUID, List<ReservationResponse>> reservationsByTable = reservationService.currentByTable(
                tables.stream().map(TableResponse::id).toList());
        return tables.stream()
                .map(table -> toActivity(table, ordersByTable.getOrDefault(table.id(), List.of()), openCalls,
                        reservationsByTable.getOrDefault(table.id(), List.of())))
                .toList();
    }

    @Override
    public TableActivityResponse getById(UUID id) {
        TableResponse table = diningTableService.getById(id);
        return toActivity(table, activeOrdersByTable().getOrDefault(id, List.of()), staffCallService.listOpen(),
                reservationService.currentByTable(List.of(id)).getOrDefault(id, List.of()));
    }

    private Map<UUID, List<OrderResponse>> activeOrdersByTable() {
        return orderService.listActiveDineIn().stream()
                .collect(Collectors.groupingBy(OrderResponse::tableId));
    }

    private TableActivityResponse toActivity(TableResponse table, List<OrderResponse> orders,
            List<StaffCallResponse> openCalls, List<ReservationResponse> reservations) {
        Set<UUID> orderIds = orders.stream().map(OrderResponse::id).collect(Collectors.toSet());
        List<StaffCallResponse> calls = openCalls.stream()
                .filter(call -> orderIds.contains(call.orderId()))
                .toList();
        return new TableActivityResponse(table, orders.size(), calls.size(), orders, calls, reservations);
    }
}
