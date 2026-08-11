package org.group1.coffeeshopapi.orders.service;

import java.util.UUID;

import java.util.List;

import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.orders.dto.request.OrderRequest;
import org.group1.coffeeshopapi.orders.dto.response.OrderResponse;

public interface OrderService {
    OrderResponse createOrder(OrderRequest request);
    List<OrderResponse> getAllOrders();
    List<OrderResponse> getMyOrders();
    OrderResponse getOrderById(UUID id);
    OrderResponse updateOrder(UUID id, OrderRequest request);
    OrderResponse updateStatus(UUID id, OrderStatus status);
    void cancel(UUID id);
    void deleteOrder(UUID id);
}
