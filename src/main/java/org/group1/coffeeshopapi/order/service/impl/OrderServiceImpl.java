package org.group1.coffeeshopapi.order.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.bakong.BakongApiClient;
import org.group1.coffeeshopapi.bakong.BakongExchangeRateService;
import org.group1.coffeeshopapi.bakong.BakongQrService;
import org.group1.coffeeshopapi.bakong.dto.BakongDeeplinkResult;
import org.group1.coffeeshopapi.bakong.dto.BakongQrResult;
import org.group1.coffeeshopapi.bakong.dto.BakongTransactionCheckResult;
import org.group1.coffeeshopapi.common.enums.Currency;
import org.group1.coffeeshopapi.common.enums.FulfillmentMethod;
import org.group1.coffeeshopapi.common.enums.OrderAuditAction;
import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.enums.PaymentMethod;
import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.common.enums.StockStrategy;
import org.group1.coffeeshopapi.common.enums.VariantLabel;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.common.properties.BakongProperties;
import org.group1.coffeeshopapi.common.properties.ShopLocationProperties;
import org.group1.coffeeshopapi.common.security.SuperAdminUserDetails;
import org.group1.coffeeshopapi.common.util.GeoUtil;
import org.group1.coffeeshopapi.extra.entity.Extra;
import org.group1.coffeeshopapi.extra.entity.ProductExtra;
import org.group1.coffeeshopapi.extra.repository.ExtraRepository;
import org.group1.coffeeshopapi.extra.repository.ProductExtraRepository;
import org.group1.coffeeshopapi.inventory.dto.request.StockCutRequest;
import org.group1.coffeeshopapi.inventory.service.InventoryService;
import org.group1.coffeeshopapi.order.dto.request.CashPaymentRequest;
import org.group1.coffeeshopapi.order.dto.request.CheckoutDetailsRequest;
import org.group1.coffeeshopapi.order.dto.request.CreateOrderRequest;
import org.group1.coffeeshopapi.order.dto.request.OrderItemRequest;
import org.group1.coffeeshopapi.order.dto.request.StaffCreateOrderRequest;
import org.group1.coffeeshopapi.order.dto.response.BakongDeeplinkResponse;
import org.group1.coffeeshopapi.order.dto.response.BakongQrResponse;
import org.group1.coffeeshopapi.order.dto.response.OrderAuditLogResponse;
import org.group1.coffeeshopapi.order.dto.response.OrderResponse;
import org.group1.coffeeshopapi.order.entity.Order;
import org.group1.coffeeshopapi.order.entity.OrderAuditLog;
import org.group1.coffeeshopapi.order.entity.OrderItem;
import org.group1.coffeeshopapi.order.entity.OrderItemExtra;
import org.group1.coffeeshopapi.order.mapper.OrderAuditLogMapper;
import org.group1.coffeeshopapi.order.mapper.OrderMapper;
import org.group1.coffeeshopapi.order.repository.OrderAuditLogRepository;
import org.group1.coffeeshopapi.order.repository.OrderRepository;
import org.group1.coffeeshopapi.order.service.OrderService;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.entity.ProductVariant;
import org.group1.coffeeshopapi.product.repository.ProductRepository;
import org.group1.coffeeshopapi.product.repository.ProductVariantRepository;
import org.group1.coffeeshopapi.product.service.ProductExtraResolver;
import org.group1.coffeeshopapi.product.service.ProductPriceResolver;
import org.group1.coffeeshopapi.product.service.ProductVariantPolicy;
import org.group1.coffeeshopapi.realtime.ChangeType;
import org.group1.coffeeshopapi.realtime.ResourceChangePublisher;
import org.group1.coffeeshopapi.realtime.ResourceType;
import org.group1.coffeeshopapi.realtime.event.OrderChangedEvent;
import org.group1.coffeeshopapi.telegram.dto.OrderInvoice;
import org.group1.coffeeshopapi.telegram.dto.OrderInvoiceLineItem;
import org.group1.coffeeshopapi.telegram.service.TelegramInvoiceService;
import org.group1.coffeeshopapi.user.dto.response.ActorSummary;
import org.group1.coffeeshopapi.user.entity.Customer;
import org.group1.coffeeshopapi.user.repository.CustomerRepository;
import org.group1.coffeeshopapi.user.service.ActorLookupService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderAuditLogRepository orderAuditLogRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductExtraRepository productExtraRepository;
    private final ExtraRepository extraRepository;
    private final InventoryService inventoryService;
    private final OrderMapper orderMapper;
    private final OrderAuditLogMapper orderAuditLogMapper;
    private final BakongQrService bakongQrService;
    private final BakongApiClient bakongApiClient;
    private final BakongExchangeRateService bakongExchangeRateService;
    private final TelegramInvoiceService telegramInvoiceService;
    private final CustomerRepository customerRepository;
    private final ActorLookupService actorLookupService;
    private final ShopLocationProperties shopLocationProperties;
    private final BakongProperties bakongProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final ResourceChangePublisher resourceChangePublisher;

    // ---------- Walk-in (POS) sales — barista or admin, ringing up their own sale ----------

    @Override
    @Transactional
    public OrderResponse create(StaffCreateOrderRequest request, UUID baristaId) {
        // Always pickup — a walk-in sale has no delivery leg.
        Order order = buildOrder(new CreateOrderRequest(request.items(), request.note()));
        order.setHandledBy(baristaId);
        order = orderRepository.save(order);
        recordChange(order, OrderAuditAction.CREATED, baristaId);
        return toResponse(order);
    }

    @Override
    public OrderResponse getOwn(UUID id, UUID baristaId) {
        return toResponse(findByHandledBy(id, baristaId));
    }

    @Override
    public Page<OrderResponse> listOwn(UUID baristaId, OrderStatus status, Pageable pageable) {
        Page<Order> orders = status == null
                ? orderRepository.findByHandledBy(baristaId, pageable)
                : orderRepository.findByHandledByAndStatus(baristaId, status, pageable);
        return toResponsePage(orders);
    }

    @Override
    @Transactional
    public OrderResponse payCash(UUID id, UUID baristaId, CashPaymentRequest request) {
        Order order = requireUnpaid(findByHandledByForUpdate(id, baristaId));
        return toResponse(chargeCash(order, request, baristaId));
    }

    @Override
    @Transactional
    public BakongQrResponse generateBakongQr(UUID id, UUID baristaId, Currency currency) {
        return attachBakongQr(requirePending(findByHandledByForUpdate(id, baristaId)), currency, baristaId);
    }

    @Override
    @Transactional
    public OrderResponse confirmBakongPayment(UUID id, UUID baristaId) {
        return toResponse(confirmBakong(findByHandledByForUpdate(id, baristaId), baristaId));
    }

    @Override
    @Transactional
    public OrderResponse cancel(UUID id, UUID baristaId) {
        Order order = requirePending(findByHandledByForUpdate(id, baristaId));
        order.setStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);
        recordChange(order, OrderAuditAction.CANCELLED, baristaId);
        return toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse collectCash(UUID id, UUID actorId, CashPaymentRequest request) {
        Order order = requireUnpaid(findAnyForUpdate(id));
        if (order.getPaymentMethod() != PaymentMethod.CASH) {
            throw new InvalidOperationException("Order is not awaiting cash collection");
        }
        return toResponse(chargeCash(order, request, actorId));
    }

    @Override
    public Page<OrderResponse> listAwaitingPickup(Pageable pageable) {
        Page<Order> orders = orderRepository.findAwaitingBaristaClaim(OrderStatus.PENDING, PaymentMethod.CASH, pageable);
        return toResponsePage(orders);
    }

    @Override
    @Transactional
    public OrderResponse acceptBakongPayment(UUID id, UUID actorId) {
        Order order = findAnyForUpdate(id);
        if (order.getPaymentMethod() != PaymentMethod.BAKONG) {
            throw new InvalidOperationException("Order is not awaiting Bakong payment");
        }
        return toResponse(confirmBakong(order, actorId));
    }

    @Override
    public Page<OrderResponse> listAwaitingBakongConfirmation(Pageable pageable) {
        Page<Order> orders = orderRepository.findAwaitingBaristaClaim(OrderStatus.PENDING, PaymentMethod.BAKONG, pageable);
        return toResponsePage(orders);
    }

    @Override
    @Transactional
    public OrderResponse setDeliveryFee(UUID id, BigDecimal fee, UUID actorId) {
        Order order = requirePending(findAnyForUpdate(id));
        if (!order.isDelivery()) {
            throw new InvalidOperationException("This order has no pinned delivery location — it's a pickup order");
        }
        order.setDeliveryFee(fee);
        recalculateTotal(order);
        // Any existing QR now encodes the wrong total — a new one has to be generated.
        clearBakongQr(order);
        order = orderRepository.save(order);
        recordChange(order, OrderAuditAction.DELIVERY_FEE_SET, actorId);
        return toResponse(order);
    }

    // ---------- Fulfillment ----------

    @Override
    @Transactional
    public OrderResponse startPreparing(UUID id, UUID actorId) {
        Order order = findAnyForUpdate(id);
        if (order.getStatus() == OrderStatus.PENDING) {
            // Cash orders can be made before they're paid; Bakong orders must clear first.
            if (order.getPaymentMethod() != PaymentMethod.CASH) {
                throw new InvalidOperationException("Order is not paid (currently pending)");
            }
            cutStockForOrder(order, actorId);
            if (order.getHandledBy() == null) {
                order.setHandledBy(actorId);
            }
        } else {
            requireStatus(order, OrderStatus.PAID);
        }
        order.setStatus(OrderStatus.PREPARING);
        order = orderRepository.save(order);
        recordChange(order, OrderAuditAction.PREPARING, actorId);
        return toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse dispatchForDelivery(UUID id, UUID actorId) {
        Order order = requireStatus(findAnyForUpdate(id), OrderStatus.PREPARING);
        if (!order.isDelivery()) {
            throw new InvalidOperationException("This is a pickup order — use complete instead");
        }
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        order.setDispatchedAt(LocalDateTime.now());
        order = orderRepository.save(order);
        recordChange(order, OrderAuditAction.OUT_FOR_DELIVERY, actorId);
        return toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse markDelivered(UUID id, UUID actorId) {
        Order order = requireStatus(findAnyForUpdate(id), OrderStatus.OUT_FOR_DELIVERY);
        requirePaymentSettled(order);
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        order = orderRepository.save(order);
        recordChange(order, OrderAuditAction.DELIVERED, actorId);
        return toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse completePickup(UUID id, UUID actorId) {
        Order order = requireStatus(findAnyForUpdate(id), OrderStatus.PREPARING);
        if (order.isDelivery()) {
            throw new InvalidOperationException("This is a delivery order — dispatch/deliver it instead");
        }
        requirePaymentSettled(order);
        order.setStatus(OrderStatus.COMPLETED);
        order = orderRepository.save(order);
        recordChange(order, OrderAuditAction.COMPLETED, actorId);
        return toResponse(order);
    }

    @Override
    public Page<OrderResponse> listAwaitingPreparation(Pageable pageable) {
        return toResponsePage(orderRepository.findAwaitingPreparation(
                OrderStatus.PAID, OrderStatus.PENDING, PaymentMethod.CASH, pageable));
    }

    @Override
    public Page<OrderResponse> listDeliveryBoard(Pageable pageable) {
        return toResponsePage(orderRepository.findByStatusForDeliveryBoard(OrderStatus.OUT_FOR_DELIVERY, pageable));
    }

    // ---------- Customer self-service orders ----------

    @Override
    @Transactional
    public OrderResponse createForCustomer(CreateOrderRequest request, UUID customerId,
            BigDecimal deliveryLatitude, BigDecimal deliveryLongitude) {
        if ((deliveryLatitude == null) != (deliveryLongitude == null)) {
            throw new InvalidOperationException("Delivery latitude and longitude must be given together");
        }
        Order order = buildOrder(request);
        order.setCustomer(customerRef(customerId));
        order.setDeliveryLatitude(deliveryLatitude);
        order.setDeliveryLongitude(deliveryLongitude);
        order = orderRepository.save(order);
        recordChange(order, OrderAuditAction.CREATED, customerId);
        return toResponse(order);
    }

    @Override
    public OrderResponse getOwnForCustomer(UUID id, UUID customerId) {
        return toResponse(findByCustomer(id, customerId));
    }

    @Override
    public Page<OrderResponse> listOwnForCustomer(UUID customerId, OrderStatus status, Pageable pageable) {
        Page<Order> orders = status == null
                ? orderRepository.findByCustomerId(customerId, pageable)
                : orderRepository.findByCustomerIdAndStatus(customerId, status, pageable);
        return toResponsePage(orders);
    }

    @Override
    @Transactional
    public OrderResponse selectCashOnPickup(UUID id, UUID customerId) {
        Order order = requirePending(findByCustomerForUpdate(id, customerId));
        order.setPaymentMethod(PaymentMethod.CASH);
        // Drop any QR so an old Bakong payment can't also be confirmed on a cash order.
        clearBakongQr(order);
        order = orderRepository.save(order);
        recordChange(order, OrderAuditAction.CASH_SELECTED, customerId);
        return toResponse(order);
    }

    @Override
    @Transactional
    public BakongQrResponse generateBakongQrForCustomer(UUID id, UUID customerId, Currency currency) {
        return attachBakongQr(requirePending(findByCustomerForUpdate(id, customerId)), currency, customerId);
    }

    @Override
    public BakongDeeplinkResponse generateBakongDeeplinkForCustomer(UUID id, UUID customerId) {
        Order order = requirePending(findByCustomer(id, customerId));
        if (order.getBakongQrString() == null) {
            throw new InvalidOperationException("No Bakong QR has been generated for this order yet");
        }
        BakongDeeplinkResult result = bakongApiClient.generateDeeplink(order.getBakongQrString());
        if (!result.success()) {
            throw new InvalidOperationException(
                    result.message() != null ? result.message() : "Could not generate a payment app link.");
        }
        return new BakongDeeplinkResponse(order.getId(), result.shortLink());
    }

    @Override
    @Transactional
    public OrderResponse confirmBakongPaymentForCustomer(UUID id, UUID customerId) {
        return toResponse(confirmBakong(findByCustomerForUpdate(id, customerId), null));
    }

    @Override
    @Transactional
    public OrderResponse cancelForCustomer(UUID id, UUID customerId) {
        Order order = requirePending(findByCustomerForUpdate(id, customerId));
        order.setStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);
        recordChange(order, OrderAuditAction.CANCELLED, customerId);
        return toResponse(order);
    }

    // ---------- Admin ----------

    @Override
    public OrderResponse getAny(UUID id) {
        return toResponse(findAny(id));
    }

    @Override
    public Page<OrderResponse> listAll(UUID baristaId, UUID customerId, OrderStatus status, Pageable pageable) {
        Page<Order> orders;
        if (baristaId != null && status != null) {
            orders = orderRepository.findByHandledByAndStatus(baristaId, status, pageable);
        } else if (baristaId != null) {
            orders = orderRepository.findByHandledBy(baristaId, pageable);
        } else if (customerId != null && status != null) {
            orders = orderRepository.findByCustomerIdAndStatus(customerId, status, pageable);
        } else if (customerId != null) {
            orders = orderRepository.findByCustomerId(customerId, pageable);
        } else if (status != null) {
            orders = orderRepository.findByStatus(status, pageable);
        } else {
            orders = orderRepository.findAllWithActors(pageable);
        }
        return toResponsePage(orders);
    }

    @Override
    @Transactional
    public OrderResponse cancelAny(UUID id, UUID actorId) {
        Order order = requirePending(findAnyForUpdate(id));
        order.setStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);
        recordChange(order, OrderAuditAction.CANCELLED, actorId);
        return toResponse(order);
    }

    @Override
    public List<OrderAuditLogResponse> getHistory(UUID orderId) {
        findAny(orderId); // 404s if the order doesn't exist
        List<OrderAuditLog> logs = orderAuditLogRepository.findByOrderIdOrderByCreatedAtAsc(orderId);

        Set<UUID> actorIds = new HashSet<>();
        for (OrderAuditLog log : logs) {
            actorIds.add(log.getActorId());
        }
        Map<UUID, ActorSummary> actors = actorLookupService.resolveAll(actorIds);

        return logs.stream()
                .map(log -> orderAuditLogMapper.toResponse(log, actors.get(log.getActorId())))
                .toList();
    }

    // ---------- Shared logic ----------

    private Order buildOrder(CreateOrderRequest request) {
        Order order = new Order();
        order.setNote(request.note());
        applyFulfillmentDetails(order, request.delivery());

        for (OrderItemRequest itemRequest : request.items()) {
            Product product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + itemRequest.productId()));
            if (product.getStatus() != Status.ACTIVE) {
                throw new InvalidOperationException("Product '" + product.getName() + "' is not available");
            }

            ProductVariant explicitVariant = resolveExplicitVariant(product, itemRequest);

            ProductVariantPolicy.validate(product, explicitVariant != null ? explicitVariant.getId() : null,
                    itemRequest.sugarLevel(), itemRequest.iceLevel(), itemRequest.milkType());

            ProductVariant variant;
            if (explicitVariant != null) {
                variant = explicitVariant;
            } else {
                List<ProductVariant> activeOptions = variantRepository
                        .findByProductIdAndStatusOrderBySortOrderAscNameAsc(product.getId(), Status.ACTIVE);
                variant = ProductPriceResolver.resolveEffective(product, null, activeOptions);
            }

            List<Extra> extras = resolveExtras(product, itemRequest.extraIds());
            BigDecimal extrasTotal = extras.stream().map(Extra::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal unitPrice = product.getFinalPrice(variant.getPrice(), LocalDateTime.now()).add(extrasTotal);
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(itemRequest.quantity()));

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setProductName(product.getName());
            item.setProductNameKh(product.getNameKh());
            item.setQuantity(itemRequest.quantity());
            item.setUnitPrice(unitPrice);
            item.setSubtotal(subtotal);
            item.setVariant(variant);
            item.setSugarLevel(itemRequest.sugarLevel());
            item.setIceLevel(itemRequest.iceLevel());
            item.setMilkType(itemRequest.milkType());
            for (Extra extra : extras) {
                OrderItemExtra orderItemExtra = new OrderItemExtra();
                orderItemExtra.setExtra(extra);
                orderItemExtra.setExtraName(extra.getName());
                orderItemExtra.setExtraPrice(extra.getPrice());
                item.addExtra(orderItemExtra);
            }
            order.addItem(item);
        }
        recalculateTotal(order);
        return order;
    }

    // Null delivery means "not given" — the order stays pickup, the default.
    private void applyFulfillmentDetails(Order order, CheckoutDetailsRequest delivery) {
        if (delivery == null) {
            return;
        }
        if (delivery.method() == FulfillmentMethod.DELIVERY
                && (delivery.address() == null || delivery.address().isBlank())) {
            throw new InvalidOperationException("A delivery address is required for delivery orders");
        }
        order.setFulfillmentMethod(delivery.method());
        order.setContactName(delivery.contactName());
        order.setContactPhone(delivery.contactPhone());
        order.setDeliveryAddress(delivery.address());
    }

    // Resolves a size by variantId or variantName (case-insensitive) — variantId wins if both are
    // given. Returns null if neither was given, so the caller can auto-resolve instead.
    private ProductVariant resolveExplicitVariant(Product product, OrderItemRequest itemRequest) {
        if (itemRequest.variantId() != null) {
            return requireActiveVariant(variantRepository
                    .findByIdAndProductId(itemRequest.variantId(), product.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Variant not found: " + itemRequest.variantId())));
        }
        if (itemRequest.variantName() != null && !itemRequest.variantName().isBlank()) {
            String name = itemRequest.variantName().trim();
            VariantLabel label;
            try {
                label = VariantLabel.valueOf(name.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ResourceNotFoundException("Variant not found: '" + name + "' for '" + product.getName() + "'");
            }
            return requireActiveVariant(variantRepository
                    .findByProductIdAndName(product.getId(), label)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Variant not found: '" + name + "' for '" + product.getName() + "'")));
        }
        return null;
    }

    private ProductVariant requireActiveVariant(ProductVariant variant) {
        if (variant.getStatus() != Status.ACTIVE) {
            throw new InvalidOperationException("Variant '" + variant.getName() + "' is not available");
        }
        return variant;
    }

    private List<Extra> resolveExtras(Product product, List<UUID> extraIds) {
        if (extraIds == null || extraIds.isEmpty()) {
            return List.of();
        }
        List<ProductExtra> attached = productExtraRepository
                .findByProductIdAndExtraIdInAndStatus(product.getId(), extraIds, Status.ACTIVE);
        return ProductExtraResolver.resolve(product, extraIds, attached);
    }

    // The one place totalAmount is computed, so it's always item subtotals plus delivery fee.
    private void recalculateTotal(Order order) {
        BigDecimal itemsTotal = order.getItems().stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal deliveryFee = order.getDeliveryFee() != null ? order.getDeliveryFee() : BigDecimal.ZERO;
        order.setTotalAmount(itemsTotal.add(deliveryFee));
    }

    private Order chargeCash(Order order, CashPaymentRequest request, UUID fulfillingActorId) {
        BigDecimal tenderedInUsd = request.currency() == Currency.KHR
                ? khrToUsd(request.amountTendered())
                : request.amountTendered();
        if (tenderedInUsd.compareTo(order.getTotalAmount()) < 0) {
            throw new InvalidOperationException("Amount tendered is less than the order total");
        }
        BigDecimal changeDueUsd = tenderedInUsd.subtract(order.getTotalAmount());
        // Defaults to whatever currency was tendered, so USD in gives USD change back — but the
        // customer can ask for the other currency instead.
        Currency changeCurrency = request.changeCurrency() != null ? request.changeCurrency() : request.currency();

        order.setHandledBy(fulfillingActorId);
        order.setPaymentMethod(PaymentMethod.CASH);
        order.setAmountTendered(request.amountTendered());
        order.setAmountTenderedCurrency(request.currency());
        order.setChangeDue(changeCurrency == Currency.KHR ? usdToKhr(changeDueUsd) : changeDueUsd);
        order.setChangeCurrency(changeCurrency);
        markPaid(order, fulfillingActorId);
        order = orderRepository.save(order);
        recordChange(order, OrderAuditAction.CASH_COLLECTED, fulfillingActorId);
        return order;
    }

    // Totals are always in USD, so a KHR cash payment has to be converted back for comparison.
    private BigDecimal khrToUsd(BigDecimal khrAmount) {
        BigDecimal rate = bakongExchangeRateService.getCurrentRate();
        if (rate == null || rate.signum() <= 0) {
            throw new InvalidOperationException("USD-to-KHR exchange rate is not configured");
        }
        return khrAmount.divide(rate, 2, RoundingMode.HALF_UP);
    }

    // KHR has no minor unit, so change given back in KHR is rounded to a whole number.
    private BigDecimal usdToKhr(BigDecimal usdAmount) {
        BigDecimal rate = bakongExchangeRateService.getCurrentRate();
        if (rate == null || rate.signum() <= 0) {
            throw new InvalidOperationException("USD-to-KHR exchange rate is not configured");
        }
        return usdAmount.multiply(rate).setScale(0, RoundingMode.HALF_UP);
    }

    private BakongQrResponse attachBakongQr(Order order, Currency currency, UUID actorId) {
        String billNumber = "ORD-" + order.getId().toString().substring(0, 8).toUpperCase();
        BakongQrResult qr = bakongQrService.generateQr(order.getTotalAmount(), billNumber, currency);

        long expiresInSeconds = bakongProperties.getExpirationMinutes() * 60;
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expiresInSeconds);

        order.setPaymentMethod(PaymentMethod.BAKONG);
        order.setBakongQrString(qr.qrString());
        order.setBakongMd5Hash(qr.md5Hash());
        order.setBakongCurrency(qr.currency());
        order.setBakongAmount(qr.amount());
        order.setBakongExpiresAt(expiresAt);
        order = orderRepository.save(order);
        recordChange(order, OrderAuditAction.BAKONG_QR_GENERATED, actorId);

        return new BakongQrResponse(order.getId(), qr.qrString(), qr.md5Hash(), qr.amount(), qr.currency(),
                expiresAt, expiresInSeconds);
    }

    // performedBy is null for a customer's own confirm — the stock cut then falls back to the
    // Super Admin's id, since stock movements always need a staff actor.
    private Order confirmBakong(Order order, UUID performedBy) {
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidOperationException("Order has been cancelled");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            // Already paid — treat a repeat confirm as a no-op.
            return order;
        }
        if (order.getPaymentMethod() != PaymentMethod.BAKONG || order.getBakongMd5Hash() == null) {
            throw new InvalidOperationException("Generate a Bakong QR for this order first");
        }

        BakongTransactionCheckResult result = bakongApiClient.checkTransactionByMd5(order.getBakongMd5Hash());
        if (result.paid()) {
            order.setBakongTransactionHash(result.transactionHash());
            if (performedBy != null) {
                order.setHandledBy(performedBy);
            }
            UUID customerId = order.getCustomer() != null ? order.getCustomer().getId() : null;
            UUID stockActorId = performedBy != null ? performedBy : SuperAdminUserDetails.ID;
            UUID auditActorId = performedBy != null ? performedBy : customerId;
            markPaid(order, stockActorId);
            order = orderRepository.save(order);
            recordChange(order, OrderAuditAction.BAKONG_CONFIRMED, auditActorId);
        }
        return order;
    }

    // Always stamps paidAt and sends the invoice. Only cuts stock and moves to PAID if that
    // hasn't already happened — a cash order already PREPARING had its stock cut back then.
    private void markPaid(Order order, UUID stockActorId) {
        if (order.getStatus() == OrderStatus.PENDING) {
            cutStockForOrder(order, stockActorId);
            order.setStatus(OrderStatus.PAID);
        }
        order.setPaidAt(LocalDateTime.now());

        if (order.getCustomer() != null) {
            telegramInvoiceService.sendInvoice(order.getCustomer().getId(), toInvoice(order));
        }
    }

    private void cutStockForOrder(Order order, UUID stockActorId) {
        for (OrderItem item : order.getItems()) {
            inventoryService.stockCut(new StockCutRequest(
                    item.getProduct().getId(),
                    BigDecimal.valueOf(item.getQuantity()),
                    StockStrategy.FIFO,
                    "Sold in order " + order.getId()), stockActorId);
            for (OrderItemExtra orderItemExtra : item.getExtras()) {
                deductExtraStock(orderItemExtra.getExtra(), item.getQuantity());
            }
        }
    }

    // Untracked (null quantityOnHand) extras are left alone; a tracked one is floored at zero.
    // A bulk update skips entity listeners, so the live update is recorded here.
    private void deductExtraStock(Extra extra, int quantitySold) {
        if (extra.getQuantityOnHand() == null) {
            return;
        }
        extraRepository.deductStock(extra.getId(), BigDecimal.valueOf(quantitySold));
        resourceChangePublisher.record(ResourceType.EXTRA, extra.getId(), ChangeType.UPDATED);
    }

    private OrderInvoice toInvoice(Order order) {
        List<OrderInvoiceLineItem> items = order.getItems().stream()
                .map(item -> new OrderInvoiceLineItem(item.getProductName(), item.getProductNameKh(), item.getQuantity(),
                        item.getUnitPrice(), item.getSubtotal(),
                        item.getExtras().stream().map(OrderItemExtra::getExtraName).toList()))
                .toList();
        return new OrderInvoice(order.getId(), items, order.getDeliveryFee(), order.getTotalAmount(),
                order.getPaymentMethod(), order.getBakongCurrency(), order.getBakongAmount(),
                order.getAmountTendered(), order.getAmountTenderedCurrency(), order.getChangeDue(),
                order.getChangeCurrency(), order.getPaidAt());
    }

    private Order requirePending(Order order) {
        return requireStatus(order, OrderStatus.PENDING);
    }

    // Looser than requirePending — a cash order can be paid any time before handover, not just
    // while still PENDING. Checks paidAt, the real source of truth, rather than status.
    private Order requireUnpaid(Order order) {
        if (order.getPaidAt() != null) {
            throw new InvalidOperationException("Payment has already been collected for this order");
        }
        if (order.getStatus().isFinished()) {
            throw new InvalidOperationException(
                    "Order is " + order.getStatus().name().toLowerCase() + " and can no longer be paid");
        }
        return order;
    }

    // A cash order that was prepared unpaid must still be paid before it's marked done.
    private void requirePaymentSettled(Order order) {
        if (order.getPaidAt() == null) {
            throw new InvalidOperationException("Collect payment for this order before completing it");
        }
    }

    private Order requireStatus(Order order, OrderStatus expected) {
        if (order.getStatus() != expected) {
            throw new InvalidOperationException(
                    "Order is not " + expected.name().toLowerCase() + " (currently " + order.getStatus().name().toLowerCase() + ")");
        }
        return order;
    }

    private Order findByHandledBy(UUID id, UUID handledBy) {
        return orderRepository.findByIdAndHandledBy(id, handledBy)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    // Row-locked lookups for anything that changes an order, so two people acting on the same
    // order at once (e.g. two baristas, or a customer and staff) run one after the other.
    private Order findAnyForUpdate(UUID id) {
        return orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    private Order findByHandledByForUpdate(UUID id, UUID handledBy) {
        return orderRepository.findByHandledByForUpdate(id, handledBy)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    private Order findByCustomerForUpdate(UUID id, UUID customerId) {
        return orderRepository.findByCustomerForUpdate(id, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    private void clearBakongQr(Order order) {
        order.setBakongQrString(null);
        order.setBakongMd5Hash(null);
        order.setBakongCurrency(null);
        order.setBakongAmount(null);
        order.setBakongExpiresAt(null);
    }

    private Order findByCustomer(UUID id, UUID customerId) {
        return orderRepository.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    private Order findAny(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    private Customer customerRef(UUID id) {
        return id != null ? customerRepository.getReferenceById(id) : null;
    }

    // Audits the change and queues a live update, sent once the transaction commits.
    private void recordChange(Order order, OrderAuditAction action, UUID actorId) {
        OrderAuditLog log = new OrderAuditLog();
        log.setOrder(order);
        log.setAction(action);
        log.setActorId(actorId);
        orderAuditLogRepository.save(log);

        // Flush so updatedAt in the pushed order reflects this change.
        orderRepository.flush();
        String customerEmail = order.getCustomer() != null ? order.getCustomer().getEmail() : null;
        eventPublisher.publishEvent(new OrderChangedEvent(action, toResponse(order), customerEmail));
    }

    private OrderResponse toResponse(Order order) {
        return orderMapper.toResponse(order, actorLookupService.resolve(order.getHandledBy()), distanceMeters(order));
    }

    private Page<OrderResponse> toResponsePage(Page<Order> orders) {
        Set<UUID> actorIds = new HashSet<>();
        for (Order order : orders) {
            if (order.getHandledBy() != null) {
                actorIds.add(order.getHandledBy());
            }
        }
        Map<UUID, ActorSummary> actors = actorLookupService.resolveAll(actorIds);
        return orders.map(order -> orderMapper.toResponse(order, actors.get(order.getHandledBy()), distanceMeters(order)));
    }

    private BigDecimal distanceMeters(Order order) {
        if (!order.isDelivery() || !shopLocationProperties.isConfigured()) {
            return null;
        }
        return GeoUtil.metersBetween(shopLocationProperties.getLatitude(), shopLocationProperties.getLongitude(),
                order.getDeliveryLatitude(), order.getDeliveryLongitude());
    }
}
