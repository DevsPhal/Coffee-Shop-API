package org.group1.coffeeshopapi.payments.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.group1.coffeeshopapi.payments.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByOrderId(UUID orderId);

    Optional<Payment> findByMd5Hash(String md5Hash);

    boolean existsByOrderId(UUID orderId);

    List<Payment> findAllByOrderByCreatedAtDesc();
}
