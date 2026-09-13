package org.group1.coffeeshopapi.admin.repository;

import org.group1.coffeeshopapi.admin.entity.Admin;
import org.group1.coffeeshopapi.common.security.SuperAdminUserDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AdminRepository extends JpaRepository<Admin, UUID> {

    // A lazy reference to actorId as an Admin, for entities that track "which admin did this" as
    // a real @ManyToOne relation — null for the Super Admin's fixed id (it deliberately has no
    // row here, see SuperAdminUserDetails) or a null id. Only valid for an id known to be
    // ADMIN/SUPER_ADMIN-scoped; a Barista's id isn't in this table either.
    default Admin referenceOrNull(UUID actorId) {
        return actorId == null || actorId.equals(SuperAdminUserDetails.ID) ? null : getReferenceById(actorId);
    }
}
