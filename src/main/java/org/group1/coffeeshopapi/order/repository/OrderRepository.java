package org.group1.coffeeshopapi.order.repository;

import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.enums.PaymentMethod;
import org.group1.coffeeshopapi.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Queries are written as explicit JPQL rather than Spring Data's derived-method naming, for two
 * reasons:
 * <ul>
 *   <li>Every {@code Page<Order>} query left-join-fetches {@code barista}/{@code customer} so
 *   listing a page of orders costs one query, not one-plus-per-row (both are lazy {@code @ManyToOne}
 *   associations, and {@link org.group1.coffeeshopapi.order.mapper.OrderMapper} reads their name
 *   off every row).</li>
 *   <li>The "no barista yet" queries below compare the association itself
 *   ({@code o.barista is null}) rather than navigating to {@code barista.id} — the latter forces
 *   an implicit join that silently drops every order with no barista at all, which is exactly the
 *   opposite of what {@link #findAwaitingBaristaClaim} needs.</li>
 * </ul>
 */
public interface OrderRepository extends JpaRepository<Order, UUID> {

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

    // The staff pickup/confirmation queue: a customer's self-service order, still PENDING, that
    // no admin/barista has claimed yet (see Order's javadoc on handledBy/customer). paymentMethod
    // picks between the cash-on-pickup queue and the Bakong-confirmation queue.
    @Query("select o from Order o left join fetch o.customer where o.customer is not null and o.handledBy is null "
            + "and o.status = :status and o.paymentMethod = :paymentMethod")
    Page<Order> findAwaitingBaristaClaim(
            @Param("status") OrderStatus status, @Param("paymentMethod") PaymentMethod paymentMethod, Pageable pageable);

    // Backs the daily report: completed sales for one barista within a day window.
    @Query("select o from Order o where o.handledBy = :handledBy and o.status = :status "
            + "and o.paidAt between :start and :end")
    List<Order> findByHandledByAndStatusAndPaidAtBetween(
            @Param("handledBy") UUID handledBy, @Param("status") OrderStatus status,
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Backs the admin-wide daily report: completed sales across every barista within a day window.
    List<Order> findByStatusAndPaidAtBetween(OrderStatus status, LocalDateTime start, LocalDateTime end);
}
