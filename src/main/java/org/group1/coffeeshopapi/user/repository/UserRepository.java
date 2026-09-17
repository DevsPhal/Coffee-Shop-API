package org.group1.coffeeshopapi.user.repository;

import org.group1.coffeeshopapi.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

// Queries across all roles at once — use this when you don't know the role ahead of time (login,
// forgot-password, ...); use AdminRepository/BaristaRepository/CustomerRepository otherwise.
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    // Role-agnostic lookup for Telegram linking — a chat can belong to any role.
    Optional<User> findByTelegramChatId(String telegramChatId);
}
