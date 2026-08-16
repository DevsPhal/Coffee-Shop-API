package org.group1.coffeeshopapi.admin.repository;

import org.group1.coffeeshopapi.admin.entity.User;
import org.group1.coffeeshopapi.common.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    Page<User> findByRole(Role role, Pageable pageable);

    Page<User> findByRoleNot(Role role, Pageable pageable);
}
