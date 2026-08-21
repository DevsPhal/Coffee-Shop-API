package org.group1.coffeeshopapi.cart.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.cart.dto.request.AddCartItemRequest;
import org.group1.coffeeshopapi.cart.dto.request.CheckoutRequest;
import org.group1.coffeeshopapi.cart.dto.request.UpdateCartItemRequest;
import org.group1.coffeeshopapi.cart.dto.response.CartItemResponse;
import org.group1.coffeeshopapi.cart.dto.response.CartResponse;
import org.group1.coffeeshopapi.cart.entity.Cart;
import org.group1.coffeeshopapi.cart.entity.CartItem;
import org.group1.coffeeshopapi.cart.repository.CartRepository;
import org.group1.coffeeshopapi.cart.service.CartService;
import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.order.dto.request.CreateOrderRequest;
import org.group1.coffeeshopapi.order.dto.request.OrderItemRequest;
import org.group1.coffeeshopapi.order.dto.response.OrderResponse;
import org.group1.coffeeshopapi.order.service.OrderService;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final OrderService orderService;

    @Override
    public CartResponse getCart(UUID customerId) {
        return toResponse(getOrCreateCart(customerId));
    }

    @Override
    @Transactional
    public CartResponse addItem(UUID customerId, AddCartItemRequest request) {
        Cart cart = getOrCreateCart(customerId);
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.productId()));
        if (product.getStatus() != Status.ACTIVE) {
            throw new InvalidOperationException("Product '" + product.getName() + "' is not available");
        }

        cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst()
                .ifPresentOrElse(
                        existing -> existing.setQuantity(existing.getQuantity() + request.quantity()),
                        () -> {
                            CartItem item = new CartItem();
                            item.setProduct(product);
                            item.setQuantity(request.quantity());
                            cart.addItem(item);
                        });

        return toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartResponse updateItem(UUID customerId, UUID itemId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCart(customerId);
        CartItem item = findItem(cart, itemId);
        item.setQuantity(request.quantity());
        return toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartResponse removeItem(UUID customerId, UUID itemId) {
        Cart cart = getOrCreateCart(customerId);
        CartItem item = findItem(cart, itemId);
        cart.getItems().remove(item);
        return toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartResponse clearCart(UUID customerId) {
        Cart cart = getOrCreateCart(customerId);
        cart.getItems().clear();
        return toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public OrderResponse checkout(UUID customerId, CheckoutRequest request) {
        Cart cart = getOrCreateCart(customerId);
        if (cart.getItems().isEmpty()) {
            throw new InvalidOperationException("Cart is empty");
        }

        List<OrderItemRequest> items = cart.getItems().stream()
                .map(item -> new OrderItemRequest(item.getProduct().getId(), item.getQuantity()))
                .toList();

        OrderResponse order = orderService.createForCustomer(new CreateOrderRequest(items, request.note()), customerId);

        cart.getItems().clear();
        cartRepository.save(cart);

        return order;
    }

    private Cart getOrCreateCart(UUID customerId) {
        return cartRepository.findByCustomerId(customerId).orElseGet(() -> {
            Cart cart = new Cart();
            cart.setCustomerId(customerId);
            return cartRepository.save(cart);
        });
    }

    private CartItem findItem(Cart cart, UUID itemId) {
        return cart.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found: " + itemId));
    }

    private CartResponse toResponse(Cart cart) {
        LocalDateTime now = LocalDateTime.now();
        BigDecimal total = BigDecimal.ZERO;
        List<CartItemResponse> items = new ArrayList<>();

        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            BigDecimal unitPrice = product.getFinalPrice(now);
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(subtotal);
            items.add(new CartItemResponse(
                    item.getId(), product.getId(), product.getName(), product.getImageUrl(),
                    unitPrice, item.getQuantity(), subtotal));
        }

        return new CartResponse(cart.getId(), items, total);
    }
}
