package org.group1.coffeeshopapi.order.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.bakong.BakongApiClient;
import org.group1.coffeeshopapi.bakong.BakongQrService;
import org.group1.coffeeshopapi.bakong.dto.BakongQrResult;
import org.group1.coffeeshopapi.bakong.dto.BakongTransactionCheckResult;
import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.enums.PaymentMethod;
import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.common.enums.StockStrategy;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.common.properties.BakongProperties;
import org.group1.coffeeshopapi.inventory.dto.request.StockCutRequest;
import org.group1.coffeeshopapi.inventory.service.InventoryService;
import org.group1.coffeeshopapi.order.dto.request.CashPaymentRequest;
import org.group1.coffeeshopapi.order.dto.request.CreateOrderRequest;
import org.group1.coffeeshopapi.order.dto.request.OrderItemRequest;
import org.group1.coffeeshopapi.order.dto.response.BakongQrResponse;
import org.group1.coffeeshopapi.order.dto.response.OrderResponse;
import org.group1.coffeeshopapi.order.entity.Order;
import org.group1.coffeeshopapi.order.entity.OrderItem;
import org.group1.coffeeshopapi.order.mapper.OrderMapper;
import org.group1.coffeeshopapi.order.repository.OrderRepository;
import org.group1.coffeeshopapi.order.service.OrderService;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final OrderMapper orderMapper;
    private final BakongQrService bakongQrService;
    private final BakongApiClient bakongApiClient;
    private final BakongProperties bakongProperties;

    // ---------- Barista (POS) sales ----------

    @Override
    @Transactional
    public OrderResponse create(CreateOrderRequest request, UUID baristaId) {
        Order order = buildOrder(request);
        order.setBaristaId(baristaId);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    public OrderResponse getOwn(UUID id, UUID baristaId) {
        return orderMapper.toResponse(findByBarista(id, baristaId));
    }

    @Override
    public Page<OrderResponse> listOwn(UUID baristaId, OrderStatus status, Pageable pageable) {
        Page<Order> orders = status == null
                ? orderRepository.findByBaristaId(baristaId, pageable)
                : orderRepository.findByBaristaIdAndStatus(baristaId, status, pageable);
        return orders.map(orderMapper::toResponse);
    }

    @Override
    @Transactional
    public OrderResponse payCash(UUID id, UUID baristaId, CashPaymentRequest request) {
        Order order = requirePending(findByBarista(id, baristaId));
        return orderMapper.toResponse(chargeCash(order, request, baristaId));
    }

    @Override
    @Transactional
    public BakongQrResponse generateBakongQr(UUID id, UUID baristaId) {
        return attachBakongQr(requirePending(findByBarista(id, baristaId)));
    }

    @Override
    @Transactional
    public OrderResponse confirmBakongPayment(UUID id, UUID baristaId) {
        return orderMapper.toResponse(confirmBakong(findByBarista(id, baristaId), baristaId));
    }

    @Override
    @Transactional
    public OrderResponse cancel(UUID id, UUID baristaId) {
        Order order = requirePending(findByBarista(id, baristaId));
        order.setStatus(OrderStatus.CANCELLED);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponse collectCash(UUID id, UUID collectingBaristaId, CashPaymentRequest request) {
        Order order = requirePending(findAny(id));
        if (order.getPaymentMethod() != PaymentMethod.CASH) {
            throw new InvalidOperationException("Order is not awaiting cash collection");
        }
        return orderMapper.toResponse(chargeCash(order, request, collectingBaristaId));
    }

    // ---------- Customer self-service orders ----------

    @Override
    @Transactional
    public OrderResponse createForCustomer(CreateOrderRequest request, UUID customerId) {
        Order order = buildOrder(request);
        order.setCustomerId(customerId);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    public OrderResponse getOwnForCustomer(UUID id, UUID customerId) {
        return orderMapper.toResponse(findByCustomer(id, customerId));
    }

    @Override
    public Page<OrderResponse> listOwnForCustomer(UUID customerId, OrderStatus status, Pageable pageable) {
        Page<Order> orders = status == null
                ? orderRepository.findByCustomerId(customerId, pageable)
                : orderRepository.findByCustomerIdAndStatus(customerId, status, pageable);
        return orders.map(orderMapper::toResponse);
    }

    @Override
    @Transactional
    public OrderResponse selectCashOnPickup(UUID id, UUID customerId) {
        Order order = requirePending(findByCustomer(id, customerId));
        order.setPaymentMethod(PaymentMethod.CASH);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public BakongQrResponse generateBakongQrForCustomer(UUID id, UUID customerId) {
        return attachBakongQr(requirePending(findByCustomer(id, customerId)));
    }

    @Override
    @Transactional
    public OrderResponse confirmBakongPaymentForCustomer(UUID id, UUID customerId) {
        return orderMapper.toResponse(confirmBakong(findByCustomer(id, customerId), null));
    }

    @Override
    @Transactional
    public OrderResponse cancelForCustomer(UUID id, UUID customerId) {
        Order order = requirePending(findByCustomer(id, customerId));
        order.setStatus(OrderStatus.CANCELLED);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    // ---------- Admin ----------

    @Override
    public OrderResponse getAny(UUID id) {
        return orderMapper.toResponse(findAny(id));
    }

    @Override
    public Page<OrderResponse> listAll(UUID baristaId, UUID customerId, OrderStatus status, Pageable pageable) {
        Page<Order> orders;
        if (baristaId != null && status != null) {
            orders = orderRepository.findByBaristaIdAndStatus(baristaId, status, pageable);
        } else if (baristaId != null) {
            orders = orderRepository.findByBaristaId(baristaId, pageable);
        } else if (customerId != null && status != null) {
            orders = orderRepository.findByCustomerIdAndStatus(customerId, status, pageable);
        } else if (customerId != null) {
            orders = orderRepository.findByCustomerId(customerId, pageable);
        } else if (status != null) {
            orders = orderRepository.findByStatus(status, pageable);
        } else {
            orders = orderRepository.findAll(pageable);
        }
        return orders.map(orderMapper::toResponse);
    }

    // ---------- Shared logic ----------

    private Order buildOrder(CreateOrderRequest request) {
        Order order = new Order();
        order.setNote(request.note());

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemRequest itemRequest : request.items()) {
            Product product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + itemRequest.productId()));
            if (product.getStatus() != Status.ACTIVE) {
                throw new InvalidOperationException("Product '" + product.getName() + "' is not available");
            }

            BigDecimal unitPrice = product.getFinalPrice(LocalDateTime.now());
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(itemRequest.quantity()));

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setProductName(product.getName());
            item.setQuantity(itemRequest.quantity());
            item.setUnitPrice(unitPrice);
            item.setSubtotal(subtotal);
            order.addItem(item);

            total = total.add(subtotal);
        }
        order.setTotalAmount(total);
        return order;
    }

    private Order chargeCash(Order order, CashPaymentRequest request, UUID fulfillingBaristaId) {
        if (request.amountTendered().compareTo(order.getTotalAmount()) < 0) {
            throw new InvalidOperationException("Amount tendered is less than the order total");
        }
        order.setBaristaId(fulfillingBaristaId);
        order.setPaymentMethod(PaymentMethod.CASH);
        order.setAmountTendered(request.amountTendered());
        order.setChangeDue(request.amountTendered().subtract(order.getTotalAmount()));
        complete(order, fulfillingBaristaId);
        return orderRepository.save(order);
    }

    private BakongQrResponse attachBakongQr(Order order) {
        String billNumber = "ORD-" + order.getId().toString().substring(0, 8).toUpperCase();
        BakongQrResult qr = bakongQrService.generateQr(order.getTotalAmount(), billNumber);

        order.setPaymentMethod(PaymentMethod.BAKONG);
        order.setBakongQrString(qr.qrString());
        order.setBakongMd5Hash(qr.md5Hash());
        orderRepository.save(order);

        return new BakongQrResponse(order.getId(), qr.qrString(), qr.md5Hash(), order.getTotalAmount(), bakongProperties.getCurrency());
    }

    // performedBy is the barista who confirms a POS sale, or null for a customer's own confirm
    // (stock movements still need a non-null attribution, so fall back to the order's barista/
    // customer id — see complete()).
    private Order confirmBakong(Order order, UUID performedBy) {
        if (order.getStatus() == OrderStatus.COMPLETED) {
            return order;
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidOperationException("Order has been cancelled");
        }
        if (order.getBakongMd5Hash() == null) {
            throw new InvalidOperationException("Generate a Bakong QR for this order first");
        }

        BakongTransactionCheckResult result = bakongApiClient.checkTransactionByMd5(order.getBakongMd5Hash());
        if (result.paid()) {
            order.setBakongTransactionHash(result.transactionHash());
            complete(order, performedBy != null ? performedBy : order.getCustomerId());
            orderRepository.save(order);
        }
        return order;
    }

    // Cuts inventory only at the point a sale is actually paid for, so a never-paid PENDING
    // order that gets cancelled leaves stock untouched.
    private void complete(Order order, UUID performedBy) {
        for (OrderItem item : order.getItems()) {
            inventoryService.stockCut(new StockCutRequest(
                    item.getProduct().getId(),
                    BigDecimal.valueOf(item.getQuantity()),
                    StockStrategy.FIFO,
                    "Sold in order " + order.getId()), performedBy);
        }
        order.setStatus(OrderStatus.COMPLETED);
        order.setPaidAt(LocalDateTime.now());
    }

    private Order requirePending(Order order) {
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOperationException("Order is not pending");
        }
        return order;
    }

    private Order findByBarista(UUID id, UUID baristaId) {
        return orderRepository.findByIdAndBaristaId(id, baristaId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    private Order findByCustomer(UUID id, UUID customerId) {
        return orderRepository.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    private Order findAny(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }
}
