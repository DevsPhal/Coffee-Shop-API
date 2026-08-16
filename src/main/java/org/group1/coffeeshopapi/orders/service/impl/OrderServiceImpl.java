package org.group1.coffeeshopapi.orders.service.impl;

import java.util.UUID;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.group1.coffeeshopapi.user.entity.User;
import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.enums.PaymentStatus;
import org.group1.coffeeshopapi.common.exception.UnauthorizedException;
import org.group1.coffeeshopapi.common.security.SecurityUtils;
import org.group1.coffeeshopapi.orders.dto.request.OrderItemRequest;
import org.group1.coffeeshopapi.orders.dto.request.OrderRequest;
import org.group1.coffeeshopapi.orders.dto.response.OrderResponse;
import org.group1.coffeeshopapi.orders.entity.Order;
import org.group1.coffeeshopapi.orders.entity.OrderItem;
import org.group1.coffeeshopapi.orders.mapper.OrderMapper;
import org.group1.coffeeshopapi.orders.repository.OrderRepository;
import org.group1.coffeeshopapi.orders.service.OrderService;
import org.group1.coffeeshopapi.payments.repository.PaymentRepository;
import org.group1.coffeeshopapi.products.entity.Product;
import org.group1.coffeeshopapi.products.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@AllArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;

    @Override
    public OrderResponse createOrder(OrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order must contain at least one item");
        }

        log.info("Order before saving: {}", request);
        Order order = orderMapper.toEntity(request);
        order.setStatus(OrderStatus.PENDING_PAYMENT);

        User customer = SecurityUtils.isCustomer() ? SecurityUtils.currentUser() : null;
        if (customer != null) {
            order.setUserId(customer.getId());
            order.setCustomerName(customer.getFullName());
        }

        applyVerifiedItems(order, request.getItems(), customer != null);
        Order saved = orderRepository.save(order);
        log.info("Order after saving: {}", saved.getId());
        return orderMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderMapper.toResponses(orderRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders() {
        UUID userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new UnauthorizedException("No authenticated customer");
        }
        return orderMapper.toResponses(orderRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID id) {
        Order order = findOrderById(id);
        assertOwnerOrStaff(order);
        return orderMapper.toResponse(order);
    }

    @Override
    public OrderResponse updateOrder(UUID id, OrderRequest request) {
        Order existing = findOrderById(id);

        if (isTerminal(existing.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot update an order that is already " + existing.getStatus());
        }
        if (request.getItems() != null && request.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order must contain at least one item");
        }

        orderMapper.updateEntity(request, existing);
        if (request.getItems() != null) {
            applyVerifiedItems(existing, request.getItems(), existing.getUserId() != null);
        }

        return orderMapper.toResponse(orderRepository.save(existing));
    }

    @Override
    public OrderResponse updateStatus(UUID id, OrderStatus status) {
        Order existing = findOrderById(id);
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order status is required");
        }
        if (existing.getStatus() == OrderStatus.SERVED && status == OrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot cancel an order that has already been served.");
        }
        if (existing.getStatus() == OrderStatus.PENDING_PAYMENT && status == OrderStatus.CONFIRMED) {
            paymentRepository.findByOrderId(existing.getId()).ifPresent(payment -> {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setVerified(true);
                paymentRepository.save(payment);
            });
        }
        existing.setStatus(status);
        return orderMapper.toResponse(orderRepository.save(existing));
    }

    @Override
    public void cancel(UUID id) {
        Order existing = findOrderById(id);
        assertOwnerOrStaff(existing);
        if (existing.getStatus() == OrderStatus.SERVED || existing.getStatus() == OrderStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot cancel an order that has already been served or completed.");
        }
        existing.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(existing);
    }

    @Override
    public void deleteOrder(UUID id) {
        Order order = findOrderById(id);
        if (order.getStatus() == OrderStatus.SERVED || order.getStatus() == OrderStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot delete an order that has already been served or completed.");
        }
        orderRepository.delete(order);
    }

    private Order findOrderById(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + id));
    }

    private void assertOwnerOrStaff(Order order) {
        if (SecurityUtils.isStaff()) {
            return;
        }
        if (!Objects.equals(order.getUserId(), SecurityUtils.currentUserId())) {
            throw new UnauthorizedException("You do not have access to this order");
        }
    }

    /**
     * Rebuilds the order's line items, overriding price/name from the product catalog whenever a
     * productId is supplied (so a client can't submit a fabricated price), then recomputes
     * totalAmount from those verified items rather than trusting a client-supplied total.
     * Customer-placed orders must reference a real product for every line - only staff (POS,
     * counter sales) may add an ad-hoc, freely priced item.
     */
    private void applyVerifiedItems(Order order, List<OrderItemRequest> itemRequests, boolean requireCatalogItem) {
        if (order.getItems() == null) {
            order.setItems(new ArrayList<>());
        }
        order.getItems().clear();
        if (itemRequests == null) {
            order.setTotalAmount(0);
            return;
        }

        List<OrderItem> items = itemRequests.stream()
                .map(itemRequest -> toVerifiedItem(itemRequest, requireCatalogItem))
                .toList();
        items.forEach(item -> item.setOrder(order));
        order.getItems().addAll(items);

        double total = items.stream().mapToDouble(item -> item.getPrice() * item.getQuantity()).sum();
        order.setTotalAmount(total);
    }

    private OrderItem toVerifiedItem(OrderItemRequest request, boolean requireCatalogItem) {
        OrderItem item = new OrderItem();
        item.setQuantity(request.getQuantity());
        if (request.getQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item quantity must be greater than zero");
        }

        if (request.getProductId() == null && requireCatalogItem) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each item must reference a valid productId");
        }

        if (request.getProductId() != null) {
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Product not found: " + request.getProductId()));
            if (Boolean.FALSE.equals(product.getIsActive())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Product is not available: " + product.getName());
            }
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setPrice(product.getPriceDollar() == null ? 0 : product.getPriceDollar().doubleValue());
        } else {
            item.setProductId(null);
            item.setProductName(request.getProductName());
            item.setPrice(request.getPrice());
        }
        return item;
    }

    private boolean isTerminal(OrderStatus status) {
        return status == OrderStatus.SERVED
                || status == OrderStatus.COMPLETED
                || status == OrderStatus.CANCELLED
                || status == OrderStatus.REFUNDED;
    }
}
