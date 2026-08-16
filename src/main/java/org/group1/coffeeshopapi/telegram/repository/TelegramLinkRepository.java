package org.group1.coffeeshopapi.telegram.repository;

import org.group1.coffeeshopapi.telegram.entity.TelegramLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TelegramLinkRepository extends JpaRepository<TelegramLink, Long> {
    Optional<TelegramLink> findByCustomerId(UUID customerId);
    Optional<TelegramLink> findByChatId(Long chatId);
    boolean existsByCustomerId(UUID customerId);
}
