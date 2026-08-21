package org.group1.coffeeshopapi.order.service;

import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.order.dto.request.CashPaymentRequest;
import org.group1.coffeeshopapi.order.dto.request.CreateOrderRequest;
import org.group1.coffeeshopapi.order.dto.response.BakongQrResponse;
import org.group1.coffeeshopapi.order.dto.response.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {

    // --- Barista (POS) sales ---

    OrderResponse create(CreateOrderRequest request, UUID baristaId);

    OrderResponse getOwn(UUID id, UUID baristaId);

    Page<OrderResponse> listOwn(UUID baristaId, OrderStatus status, Pageable pageable);

    OrderResponse payCash(UUID id, UUID baristaId, CashPaymentRequest request);

    BakongQrResponse generateBakongQr(UUID id, UUID baristaId);

    OrderResponse confirmBakongPayment(UUID id, UUID baristaId);

    OrderResponse cancel(UUID id, UUID baristaId);

    // A barista collecting cash in person for a customer's cash-on-pickup order (not one they created).
    OrderResponse collectCash(UUID id, UUID collectingBaristaId, CashPaymentRequest request);

    // --- Customer self-service orders ---

    OrderResponse createForCustomer(CreateOrderRequest request, UUID customerId);

    OrderResponse getOwnForCustomer(UUID id, UUID customerId);

    Page<OrderResponse> listOwnForCustomer(UUID customerId, OrderStatus status, Pageable pageable);

    // Customer picks "pay cash at pickup" — stays PENDING until a barista calls collectCash.
    OrderResponse selectCashOnPickup(UUID id, UUID customerId);

    BakongQrResponse generateBakongQrForCustomer(UUID id, UUID customerId);

    OrderResponse confirmBakongPaymentForCustomer(UUID id, UUID customerId);

    OrderResponse cancelForCustomer(UUID id, UUID customerId);

    // --- Admin ---

    OrderResponse getAny(UUID id);

    Page<OrderResponse> listAll(UUID baristaId, UUID customerId, OrderStatus status, Pageable pageable);
}
