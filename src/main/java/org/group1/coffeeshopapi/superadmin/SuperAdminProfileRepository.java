package org.group1.coffeeshopapi.superadmin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SuperAdminProfileRepository extends JpaRepository<SuperAdminProfile, UUID> {
}
