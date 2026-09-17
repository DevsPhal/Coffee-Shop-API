package org.group1.coffeeshopapi.admin.repository;

import org.group1.coffeeshopapi.admin.entity.Admin;
import org.group1.coffeeshopapi.common.security.SuperAdminUserDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AdminRepository extends JpaRepository<Admin, UUID> {

    // A lazy reference to actorId as an Admin — null for the Super Admin (no row here) or a null id.
    default Admin referenceOrNull(UUID actorId) {
        return actorId == null || actorId.equals(SuperAdminUserDetails.ID) ? null : getReferenceById(actorId);
    }
}
