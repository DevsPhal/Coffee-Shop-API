package org.group1.coffeeshopapi.table.repository;

import jakarta.persistence.LockModeType;
import org.group1.coffeeshopapi.common.enums.ReservationStatus;
import org.group1.coffeeshopapi.common.enums.TableStatus;
import org.group1.coffeeshopapi.table.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reservation r where r.id = :id")
    Optional<Reservation> findByIdForUpdate(@Param("id") UUID id);

    // Active bookings on the table that overlap [start, end), apart from the one being edited.
    @Query("select r from Reservation r where r.diningTable.id = :tableId and r.status in :active "
            + "and r.reservedFor < :end and r.endsAt > :start and r.id <> :excludeId order by r.reservedFor asc")
    List<Reservation> findOverlapping(@Param("tableId") UUID tableId, @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end, @Param("excludeId") UUID excludeId,
            @Param("active") Collection<ReservationStatus> active);

    // Booked parties on the table due by the given time, earliest first: the first one holds the table.
    @Query("select r from Reservation r where r.diningTable.id = :tableId and r.status = :booked "
            + "and r.reservedFor <= :dueBy order by r.reservedFor asc")
    List<Reservation> findBookedDueBy(@Param("tableId") UUID tableId, @Param("dueBy") LocalDateTime dueBy,
            @Param("booked") ReservationStatus booked);

    @Query("select r from Reservation r where r.diningTable.id = :tableId and r.status = :status "
            + "order by r.reservedFor asc")
    List<Reservation> findByTableAndStatus(@Param("tableId") UUID tableId, @Param("status") ReservationStatus status);

    // What the tables screen shows: seated parties, and bookings starting before the given time.
    @Query("select r from Reservation r join fetch r.diningTable t where t.id in :tableIds "
            + "and (r.status = :seated or (r.status = :booked and r.reservedFor < :before)) order by r.reservedFor asc")
    List<Reservation> findCurrentForTables(@Param("tableIds") Collection<UUID> tableIds,
            @Param("before") LocalDateTime before, @Param("booked") ReservationStatus booked,
            @Param("seated") ReservationStatus seated);

    @Query("select r from Reservation r left join fetch r.diningTable where r.reservedFor >= :from "
            + "and r.reservedFor < :to order by r.reservedFor asc")
    List<Reservation> findStartingBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // Booked parties whose hold has started while their table still shows as free.
    @Query("select r.id from Reservation r join r.diningTable t where r.status = :booked and r.reservedFor <= :dueBy "
            + "and t.status = :free order by r.reservedFor asc")
    List<UUID> findIdsBookedDueOnFreeTables(@Param("dueBy") LocalDateTime dueBy,
            @Param("booked") ReservationStatus booked, @Param("free") TableStatus free);

    @Query("select r.id from Reservation r where r.status = :booked and r.reservedFor < :lateBefore "
            + "order by r.reservedFor asc")
    List<UUID> findIdsBookedBefore(@Param("lateBefore") LocalDateTime lateBefore,
            @Param("booked") ReservationStatus booked);

    long countByDiningTableIdAndStatusIn(UUID tableId, Collection<ReservationStatus> statuses);

    @Query("select coalesce(max(r.partySize), 0) from Reservation r where r.diningTable.id = :tableId "
            + "and r.status = :booked")
    int findLargestBookedParty(@Param("tableId") UUID tableId, @Param("booked") ReservationStatus booked);

    @Modifying
    @Query("update Reservation r set r.diningTable = null where r.diningTable.id = :tableId")
    void detachDiningTable(@Param("tableId") UUID tableId);
}
