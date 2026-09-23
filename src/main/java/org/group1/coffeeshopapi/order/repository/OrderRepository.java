package org.group1.coffeeshopapi.order.repository;

import org.group1.coffeeshopapi.common.enums.FulfillmentMethod;
import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.enums.PaymentMethod;
import org.group1.coffeeshopapi.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Queries are explicit JPQL, not derived method names, so every Page<Order> query can
// left-join-fetch customer and avoid one query per row.
public interface OrderRepository extends JpaRepository<Order, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id and o.customer.id = :customerId")
    Optional<Order> findByCustomerForUpdate(@Param("id") UUID id, @Param("customerId") UUID customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id and o.handledBy = :handledBy")
    Optional<Order> findByHandledByForUpdate(@Param("id") UUID id, @Param("handledBy") UUID handledBy);

    @Query("select o from Order o where o.id = :id and o.handledBy = :handledBy")
    Optional<Order> findByIdAndHandledBy(@Param("id") UUID id, @Param("handledBy") UUID handledBy);

    @Query("select o from Order o left join fetch o.customer where o.handledBy = :handledBy")
    Page<Order> findByHandledBy(@Param("handledBy") UUID handledBy, Pageable pageable);

    @Query("select o from Order o left join fetch o.customer "
            + "where o.handledBy = :handledBy and o.status = :status")
    Page<Order> findByHandledByAndStatus(
            @Param("handledBy") UUID handledBy, @Param("status") OrderStatus status, Pageable pageable);

    @Query("select o from Order o where o.id = :id and o.customer.id = :customerId")
    Optional<Order> findByIdAndCustomerId(@Param("id") UUID id, @Param("customerId") UUID customerId);

    @Query("select o from Order o left join fetch o.customer where o.customer.id = :customerId")
    Page<Order> findByCustomerId(@Param("customerId") UUID customerId, Pageable pageable);

    @Query("select o from Order o left join fetch o.customer "
            + "where o.customer.id = :customerId and o.status = :status")
    Page<Order> findByCustomerIdAndStatus(
            @Param("customerId") UUID customerId, @Param("status") OrderStatus status, Pageable pageable);

    @Query("select o from Order o left join fetch o.customer where o.status = :status")
    Page<Order> findByStatus(@Param("status") OrderStatus status, Pageable pageable);

    @Query(value = "select o from Order o left join fetch o.customer",
            countQuery = "select count(o) from Order o")
    Page<Order> findAllWithActors(Pageable pageable);

    // A customer's self-service order, still PENDING, not yet claimed by staff.
    @Query("select o from Order o left join fetch o.customer where o.customer is not null and o.handledBy is null "
            + "and o.status = :status and o.paymentMethod = :paymentMethod")
    Page<Order> findAwaitingBaristaClaim(
            @Param("status") OrderStatus status, @Param("paymentMethod") PaymentMethod paymentMethod, Pageable pageable);

    // Customer delivery orders still waiting for staff to quote a fee, oldest first.
    @Query("select o from Order o left join fetch o.customer where o.status = :status "
            + "and o.customer is not null and o.deliveryFeeSetAt is null "
            + "and (o.fulfillmentMethod = :delivery or o.deliveryLatitude is not null) "
            + "order by o.createdAt asc")
    Page<Order> findAwaitingDeliveryFee(
            @Param("status") OrderStatus status, @Param("delivery") FulfillmentMethod delivery, Pageable pageable);

    // The delivery board: everything currently with a courier, oldest dispatch first.
    @Query("select o from Order o left join fetch o.customer where o.status = :status "
            + "order by o.dispatchedAt asc")
    Page<Order> findByStatusForDeliveryBoard(@Param("status") OrderStatus status, Pageable pageable);

    // The kitchen queue: PAID orders, plus unpaid PENDING cash orders (which can be started early).
    @Query("select o from Order o left join fetch o.customer where o.status = :paid "
            + "or (o.status = :pending and o.paymentMethod = :cash)")
    Page<Order> findAwaitingPreparation(
            @Param("paid") OrderStatus paid, @Param("pending") OrderStatus pending,
            @Param("cash") PaymentMethod cash, Pageable pageable);

    // Backs the daily report: paid sales for one barista within a day window. Filters on paidAt,
    // not status, since paidAt alone reliably means "this sale happened".
    @Query("select o from Order o where o.handledBy = :handledBy "
            + "and o.paidAt between :start and :end")
    List<Order> findByHandledByAndPaidAtBetween(
            @Param("handledBy") UUID handledBy,
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Backs the admin-wide daily report: paid sales across every barista within a day window.
    List<Order> findByPaidAtBetween(LocalDateTime start, LocalDateTime end);
}
