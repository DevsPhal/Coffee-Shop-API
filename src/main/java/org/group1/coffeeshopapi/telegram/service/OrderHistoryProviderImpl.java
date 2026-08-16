package org.group1.coffeeshopapi.telegram.service;

import org.group1.coffeeshopapi.orders.repository.OrderRepository;
import org.group1.coffeeshopapi.telegram.command.MyOrdersCommand;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class OrderHistoryProviderImpl implements MyOrdersCommand.OrderHistoryProvider {

    private final OrderRepository orderRepository;

    public OrderHistoryProviderImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public List<String> recentOrders(UUID customerId, int limit) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(customerId)
                .stream()
                .limit(limit)
                .map(o -> "#" + o.getOrderNumber() + " — $" + o.getTotalAmount() + " (" + o.getStatus() + ")")
                .collect(Collectors.toList());
    }
}
