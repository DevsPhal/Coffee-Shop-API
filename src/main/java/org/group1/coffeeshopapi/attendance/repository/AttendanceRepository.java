package org.group1.coffeeshopapi.attendance.repository;

import org.group1.coffeeshopapi.attendance.entity.Attendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceRepository extends JpaRepository<Attendance, UUID>, JpaSpecificationExecutor<Attendance> {

    @Query("select (count(a) > 0) from Attendance a where a.barista.id = :baristaId "
            + "and a.id <> :excludedId and a.checkInAt < :until "
            + "and (a.checkOutAt is null or a.checkOutAt > :fromTime)")
    boolean hasOverlap(@Param("baristaId") UUID baristaId, @Param("excludedId") UUID excludedId,
                       @Param("fromTime") LocalDateTime fromTime, @Param("until") LocalDateTime until);

    // The one open (not yet checked out) shift for a barista, if any — used to enforce "one open
    // shift at a time" on check-in and to find what to close on check-out.
    Optional<Attendance> findByBaristaIdAndCheckOutAtIsNull(UUID baristaId);

    Page<Attendance> findByBaristaId(UUID baristaId, Pageable pageable);
    Page<Attendance> findByCheckInAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
    Page<Attendance> findByBaristaIdAndCheckInAtBetween(UUID baristaId, LocalDateTime start, LocalDateTime end, Pageable pageable);
}
