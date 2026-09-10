package org.group1.coffeeshopapi.barista.repository;

import org.group1.coffeeshopapi.barista.entity.Barista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.UUID;

public interface BaristaRepository extends JpaRepository<Barista, UUID> {
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByPhoneNumberAndIdNot(String phoneNumber, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Barista b where b.id = :id")
    java.util.Optional<Barista> findByIdForUpdate(@Param("id") UUID id);
}
