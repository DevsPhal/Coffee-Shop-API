package org.group1.coffeeshopapi.order.repository;

import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByIdAndBaristaId(UUID id, UUID baristaId);

    Page<Order> findByBaristaId(UUID baristaId, Pageable pageable);

    Page<Order> findByBaristaIdAndStatus(UUID baristaId, OrderStatus status, Pageable pageable);

    Optional<Order> findByIdAndCustomerId(UUID id, UUID customerId);

    Page<Order> findByCustomerId(UUID customerId, Pageable pageable);

    Page<Order> findByCustomerIdAndStatus(UUID customerId, OrderStatus status, Pageable pageable);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    // Backs the daily report: completed sales for one barista within a day window.
    List<Order> findByBaristaIdAndStatusAndPaidAtBetween(
            UUID baristaId, OrderStatus status, LocalDateTime start, LocalDateTime end);

    // Backs the admin-wide daily report: completed sales across every barista within a day window.
    List<Order> findByStatusAndPaidAtBetween(OrderStatus status, LocalDateTime start, LocalDateTime end);
}
