package org.group1.coffeeshopapi.cart.repository;

import org.group1.coffeeshopapi.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {
    Optional<Cart> findByCustomer_Id(UUID customerId);
}
