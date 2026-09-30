package org.group1.coffeeshopapi.order.service;

import org.group1.coffeeshopapi.common.enums.Currency;
import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.order.dto.request.CashPaymentRequest;
import org.group1.coffeeshopapi.order.dto.request.CreateOrderRequest;
import org.group1.coffeeshopapi.order.dto.request.DeliveryLocationRequest;
import org.group1.coffeeshopapi.order.dto.request.StaffCreateOrderRequest;
import org.group1.coffeeshopapi.order.dto.response.BakongDeeplinkResponse;
import org.group1.coffeeshopapi.order.dto.response.BakongQrResponse;
import org.group1.coffeeshopapi.order.dto.response.OrderAuditLogResponse;
import org.group1.coffeeshopapi.order.dto.response.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface OrderService {

    // --- Walk-in (POS) sales — barista or admin, ringing up their own sale. Always pickup. ---

    OrderResponse create(StaffCreateOrderRequest request, UUID baristaId);

    OrderResponse getOwn(UUID id, UUID baristaId);

    Page<OrderResponse> listOwn(UUID baristaId, OrderStatus status, Pageable pageable);

    OrderResponse payCash(UUID id, UUID baristaId, CashPaymentRequest request);

    // currency is optional; null falls back to the configured default (bakong.currency).
    BakongQrResponse generateBakongQr(UUID id, UUID baristaId, Currency currency);

    OrderResponse confirmBakongPayment(UUID id, UUID baristaId);

    OrderResponse cancel(UUID id, UUID baristaId);

    // Staff collecting cash in person for a customer's cash order they didn't create.
    OrderResponse collectCash(UUID id, UUID actorId, CashPaymentRequest request);

    // Customer cash orders no staff member has claimed yet.
    Page<OrderResponse> listAwaitingPickup(Pageable pageable);

    // The Bakong counterpart to collectCash — confirms a customer's Bakong payment on their behalf.
    OrderResponse acceptBakongPayment(UUID id, UUID actorId);

    // Customer orders with a Bakong QR generated, still unclaimed by staff.
    Page<OrderResponse> listAwaitingBakongConfirmation(Pageable pageable);

    // Sets/revises the delivery fee on a pending delivery order. Rejects a pickup order or one
    // already paid.
    OrderResponse setDeliveryFee(UUID id, BigDecimal fee, UUID actorId);

    // Customer delivery orders still waiting for a fee quote — the staff alert queue.
    Page<OrderResponse> listAwaitingDeliveryFee(Pageable pageable);

    // --- Fulfillment (barista or admin, not scoped to who collected payment) ---

    // Moves an order to PREPARING. A cash order can start here straight from PENDING, unpaid —
    // cash may be collected later, at handover. A Bakong order must already be PAID.
    OrderResponse startPreparing(UUID id, UUID actorId);

    // Moves a delivery order to OUT_FOR_DELIVERY. Rejects a pickup order.
    OrderResponse dispatchForDelivery(UUID id, UUID actorId);

    // Marks a delivery as arrived. Rejects a cash order whose payment hasn't been collected yet.
    OrderResponse markDelivered(UUID id, UUID actorId);

    // Marks a pickup order as handed over. Rejects a delivery order, or a cash order that's
    // still unpaid.
    OrderResponse completePickup(UUID id, UUID actorId);

    // The kitchen queue: PAID orders plus unpaid PENDING cash orders, which are fair game to
    // start on anyway.
    Page<OrderResponse> listAwaitingPreparation(Pageable pageable);

    // The delivery board: everything currently out with a courier, oldest dispatch first.
    Page<OrderResponse> listDeliveryBoard(Pageable pageable);

    // --- Customer self-service orders ---

    // deliveryLatitude/deliveryLongitude must be given together, or not at all (pickup).
    OrderResponse createForCustomer(CreateOrderRequest request, UUID customerId,
            BigDecimal deliveryLatitude, BigDecimal deliveryLongitude);

    OrderResponse getOwnForCustomer(UUID id, UUID customerId);

    Page<OrderResponse> listOwnForCustomer(UUID customerId, OrderStatus status, Pageable pageable);

    // Customer picks to pay cash rather than Bakong — works for both pickup and delivery despite
    // the name. Stays PENDING; may get prepared before the cash is actually collected.
    OrderResponse selectCashOnPickup(UUID id, UUID customerId);

    // Pins (or moves) the delivery location on a pending order. Turns it into a delivery order
    // and resets the fee, so staff has to quote it again.
    OrderResponse pinDeliveryLocation(UUID id, UUID customerId, DeliveryLocationRequest request);

    BakongQrResponse generateBakongQrForCustomer(UUID id, UUID customerId, Currency currency);

    // Order must already have a Bakong QR generated for it.
    BakongDeeplinkResponse generateBakongDeeplinkForCustomer(UUID id, UUID customerId);

    OrderResponse confirmBakongPaymentForCustomer(UUID id, UUID customerId);

    OrderResponse cancelForCustomer(UUID id, UUID customerId);

    // --- Admin ---

    OrderResponse getAny(UUID id);

    Page<OrderResponse> listAll(UUID baristaId, UUID customerId, OrderStatus status, Pageable pageable);

    // An admin cancelling any pending order, not just ones they rang up themselves.
    OrderResponse cancelAny(UUID id, UUID actorId);

    // Full audit trail for one order — who created it, collected payment, cancelled it, etc.
    List<OrderAuditLogResponse> getHistory(UUID orderId);
}
