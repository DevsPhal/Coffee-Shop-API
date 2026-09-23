package org.group1.coffeeshopapi.extra.repository;

import org.group1.coffeeshopapi.extra.entity.Extra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

import java.util.Optional;
import java.util.UUID;

public interface ExtraRepository extends JpaRepository<Extra, UUID> {
    Optional<Extra> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);

    // One atomic statement, so two orders using the same extra at once can't both read the
    // old amount and overwrite each other. Floors at zero; untracked (null) extras are skipped.
    @Modifying
    @Query("update Extra e set e.quantityOnHand = case when e.quantityOnHand > :quantity "
            + "then e.quantityOnHand - :quantity else 0 end "
            + "where e.id = :id and e.quantityOnHand is not null")
    int deductStock(@Param("id") UUID id, @Param("quantity") BigDecimal quantity);
}
