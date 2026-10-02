package org.group1.coffeeshopapi.table.repository;

import jakarta.persistence.LockModeType;
import org.group1.coffeeshopapi.common.enums.TableStatus;
import org.group1.coffeeshopapi.table.entity.DiningTable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiningTableRepository extends JpaRepository<DiningTable, UUID> {

    Optional<DiningTable> findByTableNumberIgnoreCase(String tableNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from DiningTable t where lower(t.tableNumber) = lower(:tableNumber)")
    Optional<DiningTable> findByTableNumberForUpdate(@Param("tableNumber") String tableNumber);

    boolean existsByTableNumberIgnoreCase(String tableNumber);

    boolean existsByTableNumberIgnoreCaseAndIdNot(String tableNumber, UUID id);

    Page<DiningTable> findAllByOrderByTableNumberAsc(Pageable pageable);

    Page<DiningTable> findByStatusOrderByTableNumberAsc(TableStatus status, Pageable pageable);

    List<DiningTable> findAllByOrderByTableNumberAsc();

    List<DiningTable> findByStatusOrderByTableNumberAsc(TableStatus status);
}
