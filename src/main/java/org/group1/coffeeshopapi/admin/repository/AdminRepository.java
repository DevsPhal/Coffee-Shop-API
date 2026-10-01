package org.group1.coffeeshopapi.admin.repository;

import org.group1.coffeeshopapi.admin.entity.Admin;
import org.group1.coffeeshopapi.common.security.SuperAdminUserDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AdminRepository extends JpaRepository<Admin, UUID> {

    default Admin referenceOrNull(UUID actorId) {
        return actorId == null || actorId.equals(SuperAdminUserDetails.ID) ? null : getReferenceById(actorId);
    }
}
