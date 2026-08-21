package org.group1.coffeeshopapi.cart.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.cart.dto.request.AddCartItemRequest;
import org.group1.coffeeshopapi.cart.dto.request.CheckoutRequest;
import org.group1.coffeeshopapi.cart.dto.request.UpdateCartItemRequest;
import org.group1.coffeeshopapi.cart.dto.response.CartResponse;
import org.group1.coffeeshopapi.cart.service.CartService;
import org.group1.coffeeshopapi.common.constant.AppConstant;
import org.group1.coffeeshopapi.common.response.ApiResponse;
import org.group1.coffeeshopapi.common.security.CustomUserDetails;
import org.group1.coffeeshopapi.order.dto.response.OrderResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/customer/cart")
@RequiredArgsConstructor
@Tag(name = "Customer Cart", description = "Customer only: add products to cart and checkout")
@SecurityRequirement(name = "bearerAuth")
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ApiResponse<CartResponse> getCart(@AuthenticationPrincipal CustomUserDetails currentUser) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, cartService.getCart(currentUser.getId()));
    }

    @PostMapping("/items")
    public ApiResponse<CartResponse> addItem(
            @Valid @RequestBody AddCartItemRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ApiResponse.of(HttpStatus.OK, "Item added to cart.", cartService.addItem(currentUser.getId(), request));
    }

    @PatchMapping("/items/{itemId}")
    public ApiResponse<CartResponse> updateItem(
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateCartItemRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ApiResponse.of(HttpStatus.OK, "Cart item updated.", cartService.updateItem(currentUser.getId(), itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ApiResponse<CartResponse> removeItem(
            @PathVariable UUID itemId, @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ApiResponse.of(HttpStatus.OK, "Cart item removed.", cartService.removeItem(currentUser.getId(), itemId));
    }

    @DeleteMapping
    public ApiResponse<CartResponse> clearCart(@AuthenticationPrincipal CustomUserDetails currentUser) {
        return ApiResponse.of(HttpStatus.OK, "Cart cleared.", cartService.clearCart(currentUser.getId()));
    }

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(
            @RequestBody(required = false) CheckoutRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        CheckoutRequest checkoutRequest = request != null ? request : new CheckoutRequest(null);
        OrderResponse response = cartService.checkout(currentUser.getId(), checkoutRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(HttpStatus.CREATED, "Order placed successfully.", response));
    }
}
