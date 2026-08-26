package org.group1.coffeeshopapi.order.repository;

import org.group1.coffeeshopapi.order.entity.OrderAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderAuditLogRepository extends JpaRepository<OrderAuditLog, UUID> {

    List<OrderAuditLog> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
}
