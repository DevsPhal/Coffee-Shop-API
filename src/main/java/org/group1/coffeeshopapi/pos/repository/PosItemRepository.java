package org.group1.coffeeshopapi.pos.repository;

import org.group1.coffeeshopapi.pos.entity.PosItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PosItemRepository extends JpaRepository<PosItem, UUID> {
    List<PosItem> findAllByActiveTrue();
}
