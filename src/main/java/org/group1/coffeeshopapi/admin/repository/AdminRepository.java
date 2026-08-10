package org.group1.coffeeshopapi.admin.repository;


import org.group1.coffeeshopapi.admin.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdminRepository extends JpaRepository<Admin, UUID> {
   Optional<Admin> findByUserId(UUID userId);

   boolean existsByUserId(UUID userId);
}
