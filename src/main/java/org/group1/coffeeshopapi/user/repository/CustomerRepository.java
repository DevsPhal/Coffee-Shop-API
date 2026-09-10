package org.group1.coffeeshopapi.user.repository;

import org.group1.coffeeshopapi.user.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByEmail(String email);
    Optional<Customer> findByTelegramChatId(String telegramChatId);

    // Phone numbers carry a unique constraint (uk_customers_phone_number). Checking up front
    // turns a duplicate into a clean 409 instead of letting the insert fail as a 500.
    boolean existsByPhoneNumber(String phoneNumber);

    // Broadcast targets for Telegram announcements (new events, reminders, ...).
    List<Customer> findByTelegramChatIdIsNotNull();
}