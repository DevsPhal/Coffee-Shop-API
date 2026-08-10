package org.group1.coffeeshopapi.orders.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.responses.ApiResponse;
import org.group1.coffeeshopapi.orders.dto.request.OrderRequest;
import org.group1.coffeeshopapi.orders.dto.response.OrderResponse;
import org.group1.coffeeshopapi.orders.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','BARISTA','CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<OrderResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Order created successfully")
                .data(orderService.createOrder(request))
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','BARISTA')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.<List<OrderResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Orders retrieved successfully")
                .data(orderService.getAllOrders())
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMine() {
        return ResponseEntity.ok(ApiResponse.<List<OrderResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Your orders retrieved successfully")
                .data(orderService.getMyOrders())
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BARISTA','CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Order retrieved successfully")
                .data(orderService.getOrderById(id))
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BARISTA')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrder(@PathVariable Long id, @RequestBody OrderRequest request) {
        return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Order updated successfully")
                .data(orderService.updateOrder(id, request))
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','BARISTA')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(@PathVariable Long id, @RequestBody StatusRequest request) {
        return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Order status updated successfully")
                .data(orderService.updateStatus(id, request.status()))
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Order deleted successfully")
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','BARISTA','CUSTOMER')")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable Long id) {
        orderService.cancel(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Order cancelled successfully")
                .timeStamp(LocalDateTime.now())
                .build());
    }

    public record StatusRequest(OrderStatus status) {}
}
