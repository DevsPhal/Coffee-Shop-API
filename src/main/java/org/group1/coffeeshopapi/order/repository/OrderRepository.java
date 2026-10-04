package org.group1.coffeeshopapi.order.repository;

import org.group1.coffeeshopapi.common.enums.FulfillmentMethod;
import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.enums.PaymentMethod;
import org.group1.coffeeshopapi.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    @Query("select o.id from Order o where o.status = :status and o.paymentMethod = :paymentMethod "
            + "and o.bakongMd5Hash is not null "
            + "and (o.bakongExpiresAt >= :payableSince or o.bakongPreviousMd5Hashes is not null) "
            + "order by o.createdAt")
    List<UUID> findIdsAwaitingBakongPayment(
            @Param("status") OrderStatus status, @Param("paymentMethod") PaymentMethod paymentMethod,
            @Param("payableSince") LocalDateTime payableSince);

    @Query("select o.id from Order o where o.status = :status and o.paymentMethod = :paymentMethod "
            + "and o.customer is not null and o.bakongMd5Hash is not null and o.bakongExpiresAt < :expiredBefore "
            + "order by o.createdAt")
    List<UUID> findIdsWithExpiredBakongQr(
            @Param("status") OrderStatus status, @Param("paymentMethod") PaymentMethod paymentMethod,
            @Param("expiredBefore") LocalDateTime expiredBefore);

    @Query("select o from Order o left join fetch o.customer where o.customer is not null and o.handledBy is null "
            + "and o.status = :status and o.paymentMethod = :paymentMethod")
    Page<Order> findAwaitingBaristaClaim(
            @Param("status") OrderStatus status, @Param("paymentMethod") PaymentMethod paymentMethod, Pageable pageable);

    @Query("select o from Order o left join fetch o.customer where o.customer is not null "
            + "and o.paymentMethod = :cash and o.paidAt is null "
            + "and (o.status = :pending or (o.status = :preparing and o.fulfillmentMethod <> :delivery)) "
            + "order by o.createdAt asc")
    Page<Order> findAwaitingCashCollection(
            @Param("cash") PaymentMethod cash, @Param("pending") OrderStatus pending,
            @Param("preparing") OrderStatus preparing, @Param("delivery") FulfillmentMethod delivery,
            Pageable pageable);

    @Query("select o from Order o left join fetch o.customer where o.status = :status "
            + "and o.customer is not null and o.deliveryFeeSetAt is null "
            + "and (o.fulfillmentMethod = :delivery or o.deliveryLatitude is not null) "
            + "order by o.createdAt asc")
    Page<Order> findAwaitingDeliveryFee(
            @Param("status") OrderStatus status, @Param("delivery") FulfillmentMethod delivery, Pageable pageable);

    @Query("select o from Order o left join fetch o.customer where o.status = :status "
            + "order by o.dispatchedAt asc")
    Page<Order> findByStatusForDeliveryBoard(@Param("status") OrderStatus status, Pageable pageable);

    @Query("select o from Order o left join fetch o.customer where o.status = :paid "
            + "or (o.status = :pending and o.paymentMethod = :cash)")
    Page<Order> findAwaitingPreparation(
            @Param("paid") OrderStatus paid, @Param("pending") OrderStatus pending,
            @Param("cash") PaymentMethod cash, Pageable pageable);

    @Query("select o from Order o where o.handledBy = :handledBy "
            + "and o.paidAt >= :start and o.paidAt < :end")
    List<Order> findPaidByHandledByInRange(
            @Param("handledBy") UUID handledBy,
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("select o from Order o left join fetch o.customer where o.customer.id = :customerId "
            + "and o.diningTable.id = :tableId and o.status not in :finished order by o.createdAt desc")
    List<Order> findActiveByCustomerIdAndTableId(
            @Param("customerId") UUID customerId, @Param("tableId") UUID tableId,
            @Param("finished") List<OrderStatus> finished);

    @Query("select o from Order o left join fetch o.customer where o.diningTable.id = :tableId "
            + "and o.status not in :finished order by o.createdAt asc")
    List<Order> findActiveByTableId(
            @Param("tableId") UUID tableId, @Param("finished") List<OrderStatus> finished);

    @Query("select o from Order o left join fetch o.customer where o.diningTable is not null "
            + "and o.status not in :finished order by o.createdAt asc")
    List<Order> findActiveDineIn(@Param("finished") List<OrderStatus> finished);

    @Modifying
    @Query("update Order o set o.diningTable = null where o.diningTable.id = :tableId")
    void detachDiningTable(@Param("tableId") UUID tableId);

    @Query("select o from Order o where o.paidAt >= :start and o.paidAt < :end")
    List<Order> findPaidInRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
