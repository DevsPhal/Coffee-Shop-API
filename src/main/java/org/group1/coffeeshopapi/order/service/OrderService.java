package org.group1.coffeeshopapi.order.service;

import org.group1.coffeeshopapi.common.enums.Currency;
import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.order.dto.request.CashPaymentRequest;
import org.group1.coffeeshopapi.order.dto.request.CreateOrderRequest;
import org.group1.coffeeshopapi.order.dto.response.BakongQrResponse;
import org.group1.coffeeshopapi.order.dto.response.OrderAuditLogResponse;
import org.group1.coffeeshopapi.order.dto.response.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface OrderService {

    // --- Barista (POS) sales ---

    OrderResponse create(CreateOrderRequest request, UUID baristaId);

    OrderResponse getOwn(UUID id, UUID baristaId);

    Page<OrderResponse> listOwn(UUID baristaId, OrderStatus status, Pageable pageable);

    OrderResponse payCash(UUID id, UUID baristaId, CashPaymentRequest request);

    // currency is optional; null falls back to the configured default (bakong.currency).
    BakongQrResponse generateBakongQr(UUID id, UUID baristaId, Currency currency);

    OrderResponse confirmBakongPayment(UUID id, UUID baristaId);

    OrderResponse cancel(UUID id, UUID baristaId);

    // Staff (barista or admin) collecting cash in person for a customer's cash-on-pickup order
    // (not one they created).
    OrderResponse collectCash(UUID id, UUID actorId, CashPaymentRequest request);

    // The pickup queue: customer cash-on-pickup orders no staff member has claimed yet — what
    // collectCash above is meant to be called on.
    Page<OrderResponse> listAwaitingPickup(Pageable pageable);

    // The Bakong counterpart to collectCash: staff confirming/accepting a customer's Bakong-paid
    // order they didn't create. Checks payment status the same way the customer's own confirm
    // does, and attributes the order to this staff member once paid.
    OrderResponse acceptBakongPayment(UUID id, UUID actorId);

    // The Bakong counterpart to listAwaitingPickup: customer orders with a Bakong QR generated,
    // still PENDING, that no staff member has claimed yet.
    Page<OrderResponse> listAwaitingBakongConfirmation(Pageable pageable);

    // --- Customer self-service orders ---

    OrderResponse createForCustomer(CreateOrderRequest request, UUID customerId);

    OrderResponse getOwnForCustomer(UUID id, UUID customerId);

    Page<OrderResponse> listOwnForCustomer(UUID customerId, OrderStatus status, Pageable pageable);

    // Customer picks "pay cash at pickup" — stays PENDING until a barista calls collectCash.
    OrderResponse selectCashOnPickup(UUID id, UUID customerId);

    BakongQrResponse generateBakongQrForCustomer(UUID id, UUID customerId, Currency currency);

    OrderResponse confirmBakongPaymentForCustomer(UUID id, UUID customerId);

    OrderResponse cancelForCustomer(UUID id, UUID customerId);

    // --- Admin ---

    OrderResponse getAny(UUID id);

    Page<OrderResponse> listAll(UUID baristaId, UUID customerId, OrderStatus status, Pageable pageable);

    // An admin cancelling any pending order (not scoped to one they created), unlike cancel()
    // above which only cancels orders the caller themselves rang up.
    OrderResponse cancelAny(UUID id, UUID actorId);

    // The full handling audit trail for one order — created / cash collected / Bakong confirmed /
    // cancelled, each with who did it — since handledBy on the order itself only ever holds the
    // most recent actor.
    List<OrderAuditLogResponse> getHistory(UUID orderId);
}
