package org.group1.coffeeshopapi.orders.service;

import java.util.List;

import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.orders.dto.request.OrderRequest;
import org.group1.coffeeshopapi.orders.dto.response.OrderResponse;

public interface OrderService {
    OrderResponse createOrder(OrderRequest request);
    List<OrderResponse> getAllOrders();
    List<OrderResponse> getMyOrders();
    OrderResponse getOrderById(Long id);
    OrderResponse updateOrder(Long id, OrderRequest request);
    OrderResponse updateStatus(Long id, OrderStatus status);
    void cancel(Long id);
    void deleteOrder(Long id);
}
