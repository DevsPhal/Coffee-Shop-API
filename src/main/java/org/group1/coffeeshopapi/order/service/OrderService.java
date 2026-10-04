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

    OrderResponse create(StaffCreateOrderRequest request, UUID baristaId);

    OrderResponse getOwn(UUID id, UUID baristaId);

    Page<OrderResponse> listOwn(UUID baristaId, OrderStatus status, Pageable pageable);

    OrderResponse payCash(UUID id, UUID baristaId, CashPaymentRequest request);

    BakongQrResponse generateBakongQr(UUID id, UUID baristaId, Currency currency);

    OrderResponse confirmBakongPayment(UUID id, UUID baristaId);

    OrderResponse cancel(UUID id, UUID baristaId);

    OrderResponse collectCash(UUID id, UUID actorId, CashPaymentRequest request);

    Page<OrderResponse> listAwaitingPickup(Pageable pageable);

    OrderResponse acceptBakongPayment(UUID id, UUID actorId);

    Page<OrderResponse> listAwaitingBakongConfirmation(Pageable pageable);

    OrderResponse setDeliveryFee(UUID id, BigDecimal fee, UUID actorId);

    OrderResponse setEstimatedTime(UUID id, int minutes, UUID actorId);

    Page<OrderResponse> listAwaitingDeliveryFee(Pageable pageable);

    OrderResponse startPreparing(UUID id, UUID actorId);

    OrderResponse dispatchForDelivery(UUID id, UUID actorId);

    OrderResponse markDelivered(UUID id, UUID actorId);

    OrderResponse completePickup(UUID id, UUID actorId);

    Page<OrderResponse> listAwaitingPreparation(Pageable pageable);

    Page<OrderResponse> listDeliveryBoard(Pageable pageable);

    OrderResponse createForCustomer(CreateOrderRequest request, UUID customerId,
            BigDecimal deliveryLatitude, BigDecimal deliveryLongitude);

    OrderResponse getOwnForCustomer(UUID id, UUID customerId);

    Page<OrderResponse> listOwnForCustomer(UUID customerId, OrderStatus status, Pageable pageable);

    List<OrderResponse> listActiveAtTableForCustomer(String tableNumber, UUID customerId);

    List<OrderResponse> listActiveDineIn();

    List<OrderResponse> listActiveAtTable(UUID tableId);

    OrderResponse selectCashOnPickup(UUID id, UUID customerId);

    OrderResponse pinDeliveryLocation(UUID id, UUID customerId, DeliveryLocationRequest request);

    BakongQrResponse generateBakongQrForCustomer(UUID id, UUID customerId, Currency currency);

    BakongDeeplinkResponse generateBakongDeeplinkForCustomer(UUID id, UUID customerId);

    OrderResponse confirmBakongPaymentForCustomer(UUID id, UUID customerId);

    /** {@code promptly}: the customer tapped "I've paid", so Bakong may be asked again sooner. */
    OrderResponse confirmBakongPaymentForCustomer(UUID id, UUID customerId, boolean promptly);

    List<UUID> listOrdersAwaitingBakongPayment();

    boolean confirmBakongPaymentAutomatically(UUID id);

    /** Online orders still unpaid after their QR payment window closed. */
    List<UUID> listExpiredBakongOrders();

    /**
     * Cancels an online order whose payment window has closed, after a final check with Bakong shows it
     * wasn't paid. Throws PaymentVerificationUnavailableException when Bakong can't be checked.
     */
    boolean cancelExpiredBakongOrder(UUID id);

    OrderResponse cancelForCustomer(UUID id, UUID customerId);

    OrderResponse getAny(UUID id);

    Page<OrderResponse> listAll(UUID baristaId, UUID customerId, OrderStatus status, Pageable pageable);

    OrderResponse cancelAny(UUID id, UUID actorId);

    List<OrderAuditLogResponse> getHistory(UUID orderId);
}
