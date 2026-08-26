package org.group1.coffeeshopapi.user.repository;

import org.group1.coffeeshopapi.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Queries across ALL roles at once (Hibernate unions the {@code admins}, {@code baristas}, and
 * {@code customers} tables under {@code TABLE_PER_CLASS} inheritance). Use this for auth flows
 * that don't know the role ahead of time (login, forgot-password, ...); use
 * AdminRepository/BaristaRepository/CustomerRepository when you already know the role.
 */
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
