package org.group1.coffeeshopapi.extra.repository;

import org.group1.coffeeshopapi.extra.entity.Extra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ExtraRepository extends JpaRepository<Extra, UUID> {
    Optional<Extra> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
}
