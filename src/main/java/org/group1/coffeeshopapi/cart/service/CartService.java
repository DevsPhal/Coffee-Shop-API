package org.group1.coffeeshopapi.cart.service;

import org.group1.coffeeshopapi.cart.dto.request.AddCartItemRequest;
import org.group1.coffeeshopapi.cart.dto.request.CheckoutRequest;
import org.group1.coffeeshopapi.cart.dto.request.UpdateCartItemRequest;
import org.group1.coffeeshopapi.cart.dto.response.CartResponse;
import org.group1.coffeeshopapi.order.dto.response.OrderResponse;

import java.util.UUID;

public interface CartService {
    CartResponse getCart(UUID customerId);

    CartResponse addItem(UUID customerId, AddCartItemRequest request);

    CartResponse updateItem(UUID customerId, UUID itemId, UpdateCartItemRequest request);

    CartResponse removeItem(UUID customerId, UUID itemId);

    CartResponse clearCart(UUID customerId);

    OrderResponse checkout(UUID customerId, CheckoutRequest request);
}
