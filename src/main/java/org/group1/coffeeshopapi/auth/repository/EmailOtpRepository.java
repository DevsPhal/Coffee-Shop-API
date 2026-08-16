package org.group1.coffeeshopapi.auth.repository;

import org.group1.coffeeshopapi.auth.entity.EmailOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface EmailOtpRepository extends JpaRepository<EmailOtp, UUID> {
    Optional<EmailOtp> findTopByEmailAndConsumedAtIsNullOrderByCreatedAtDesc(String email);

    void deleteByEmail(String email);

    void deleteByExpiresAtBefore(LocalDateTime expiresAt);
}
