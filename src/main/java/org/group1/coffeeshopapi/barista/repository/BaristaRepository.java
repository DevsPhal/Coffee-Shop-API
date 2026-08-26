package org.group1.coffeeshopapi.barista.repository;

import org.group1.coffeeshopapi.barista.entity.Barista;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BaristaRepository extends JpaRepository<Barista, UUID> {
}