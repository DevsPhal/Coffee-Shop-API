package org.group1.coffeeshopapi.user.repository;

import org.group1.coffeeshopapi.common.security.SuperAdminUserDetails;
import org.group1.coffeeshopapi.user.entity.SuperAdminProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SuperAdminProfileRepository extends JpaRepository<SuperAdminProfile, UUID> {

    default Optional<SuperAdminProfile> findProfile() {
        return findById(SuperAdminUserDetails.ID);
    }

    /** The name the super admin chose, or the built-in "Super Admin" until one is saved. */
    default String displayName() {
        return findProfile()
                .map(SuperAdminProfile::getFullName)
                .filter(name -> !name.isBlank())
                .orElse(SuperAdminUserDetails.DISPLAY_NAME);
    }
}
