package org.group1.coffeeshopapi.orders.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.group1.coffeeshopapi.admin.entity.User;
import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.exception.InvalidRequestException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
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
import org.group1.coffeeshopapi.products.entity.Product;
import org.group1.coffeeshopapi.products.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ProductRepository productRepository;

    @Override
    public OrderResponse createOrder(OrderRequest request) {
        Order order = orderMapper.toEntity(request);

        User customer = SecurityUtils.isCustomer() ? SecurityUtils.currentUser() : null;
        if (customer != null) {
            order.setUserId(customer.getId());
            order.setCustomerName(customer.getFullName());
        }

        applyVerifiedItems(order, request.getItems(), customer != null);
        Order saved = orderRepository.save(order);
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
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new UnauthorizedException("No authenticated customer");
        }
        return orderMapper.toResponses(orderRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = findOrderById(id);
        assertOwnerOrStaff(order);
        return orderMapper.toResponse(order);
    }

    @Override
    public OrderResponse updateOrder(Long id, OrderRequest request) {
        Order existing = findOrderById(id);

        orderMapper.updateEntity(request, existing);
        if (request.getItems() != null) {
            applyVerifiedItems(existing, request.getItems(), existing.getUserId() != null);
        }

        return orderMapper.toResponse(orderRepository.save(existing));
    }

    @Override
    public OrderResponse updateStatus(Long id, OrderStatus status) {
        Order existing = findOrderById(id);
        existing.setStatus(status);
        return orderMapper.toResponse(orderRepository.save(existing));
    }

    @Override
    public void cancel(Long id) {
        Order existing = findOrderById(id);
        assertOwnerOrStaff(existing);
        existing.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(existing);
    }

    @Override
    public void deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new ResourceNotFoundException("Order not found: " + id);
        }
        orderRepository.deleteById(id);
    }

    private Order findOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
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

        if (request.getProductId() == null && requireCatalogItem) {
            throw new InvalidRequestException("Each item must reference a valid productId");
        }

        if (request.getProductId() != null) {
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.getProductId()));
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
}
