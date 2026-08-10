package org.group1.coffeeshopapi.pos.repository;

import org.group1.coffeeshopapi.pos.entity.PosItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PosItemRepository extends JpaRepository<PosItem, Long> {
    List<PosItem> findAllByActiveTrue();
}
