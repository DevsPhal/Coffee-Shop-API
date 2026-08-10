package org.group1.coffeeshopapi.payments.repository;

import java.util.List;
import java.util.Optional;

import org.group1.coffeeshopapi.payments.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByMd5Hash(String md5Hash);

    boolean existsByOrderId(Long orderId);

    List<Payment> findAllByOrderByCreatedAtDesc();
}
