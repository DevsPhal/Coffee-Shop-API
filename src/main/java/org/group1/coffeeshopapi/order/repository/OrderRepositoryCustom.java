package org.group1.coffeeshopapi.order.repository;

import org.group1.coffeeshopapi.order.entity.Order;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public interface OrderRepositoryCustom {

    /** How many matching orders each status has and what they add up to; statuses with none are left out. */
    List<OrderStatusTotal> totalsByStatus(Specification<Order> specification);
}
