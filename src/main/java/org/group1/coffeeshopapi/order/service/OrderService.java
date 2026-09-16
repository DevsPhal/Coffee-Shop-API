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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface OrderService {

    // --- Walk-in (POS) sales — barista or admin, ringing up their own sale ---

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

    // Staff (barista or admin) evaluating/revising the delivery fee for any still-PENDING delivery
    // order (not scoped to one they've claimed) — folded into totalAmount immediately. Rejects an
    // order with no pinned delivery location (see Order.isDelivery) or one already paid for.
    OrderResponse setDeliveryFee(UUID id, BigDecimal fee, UUID actorId);

    // --- Fulfillment (barista or admin, not scoped to who collected payment — see OrderStatus
    // for the full lifecycle each of these moves an order through) ---

    // PAID -> PREPARING, same as always for a Bakong order. A cash order can instead go straight
    // from PENDING -> PREPARING with no payment yet — the real-world "make it, collect at
    // handover" flow for cash-on-pickup and cash-on-delivery — cutting stock right here instead of
    // at PAID, since PAID may never happen for this order. Rejects a still-PENDING Bakong order.
    OrderResponse startPreparing(UUID id, UUID actorId);

    // PREPARING -> OUT_FOR_DELIVERY: the order has left the shop with a courier. Delivery orders
    // only — rejects a pickup order (see Order.isDelivery). A cash order's payment need not be
    // collected yet — that can happen on arrival.
    OrderResponse dispatchForDelivery(UUID id, UUID actorId);

    // OUT_FOR_DELIVERY -> DELIVERED: the courier confirms it arrived. Terminal. Rejects a cash
    // order whose payment hasn't been collected yet — collect it (collectCash) before delivering.
    OrderResponse markDelivered(UUID id, UUID actorId);

    // PREPARING -> COMPLETED: handed to the customer at the counter. Pickup orders only — rejects
    // a delivery order (dispatch/deliver it instead). Terminal. Rejects a cash order whose payment
    // hasn't been collected yet — collect it (collectCash) before completing.
    OrderResponse completePickup(UUID id, UUID actorId);

    // The kitchen queue: everything a barista can start making right now — orders already PAID,
    // plus PENDING cash orders that haven't been paid yet but are fair game to start on anyway.
    Page<OrderResponse> listAwaitingPreparation(Pageable pageable);

    // The delivery board: everything currently out with a courier, oldest dispatch first.
    Page<OrderResponse> listDeliveryBoard(Pageable pageable);

    // --- Customer self-service orders ---

    // deliveryLatitude/deliveryLongitude are optional and must both be given together — null for
    // both means a pickup order, the shop's default.
    OrderResponse createForCustomer(CreateOrderRequest request, UUID customerId,
            BigDecimal deliveryLatitude, BigDecimal deliveryLongitude);

    OrderResponse getOwnForCustomer(UUID id, UUID customerId);

    Page<OrderResponse> listOwnForCustomer(UUID customerId, OrderStatus status, Pageable pageable);

    // Customer picks to pay cash rather than Bakong — works the same for pickup and delivery
    // orders despite the name. Stays PENDING; a barista may start preparing it before its cash is
    // ever collected (see startPreparing), with collectCash settling payment whenever it happens.
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
