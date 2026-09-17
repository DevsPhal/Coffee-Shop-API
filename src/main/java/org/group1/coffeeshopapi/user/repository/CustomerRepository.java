package org.group1.coffeeshopapi.user.repository;

import org.group1.coffeeshopapi.user.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByEmail(String email);
    Optional<Customer> findByTelegramChatId(String telegramChatId);

    // Checked up front so a duplicate phone number gets a clean error, not a raw DB failure.
    boolean existsByPhoneNumber(String phoneNumber);

    // Broadcast targets for Telegram announcements (new events, reminders, ...).
    List<Customer> findByTelegramChatIdIsNotNull();
}