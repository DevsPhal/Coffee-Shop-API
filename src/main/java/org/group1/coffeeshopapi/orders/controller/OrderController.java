package org.group1.coffeeshopapi.orders.controller;

import java.util.UUID;

import java.util.List;

import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.orders.dto.request.OrderRequest;
import org.group1.coffeeshopapi.orders.dto.response.OrderResponse;
import org.group1.coffeeshopapi.orders.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','BARISTA','CUSTOMER')")
    public OrderResponse createOrder(@RequestBody OrderRequest request) {
        return orderService.createOrder(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','BARISTA')")
    public List<OrderResponse> getAll() {
        return orderService.getAllOrders();
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<OrderResponse> getMine() {
        return orderService.getMyOrders();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BARISTA','CUSTOMER')")
    public OrderResponse getById(@PathVariable UUID id) {
        return orderService.getOrderById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BARISTA')")
    public OrderResponse updateOrder(@PathVariable UUID id, @RequestBody OrderRequest request) {
        return orderService.updateOrder(id, request);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','BARISTA')")
    public OrderResponse updateStatus(@PathVariable UUID id, @RequestBody StatusRequest request) {
        return orderService.updateStatus(id, request.status());
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteOrder(@PathVariable UUID id) {
        orderService.deleteOrder(id);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','BARISTA','CUSTOMER')")
    public void cancel(@PathVariable UUID id) {
        orderService.cancel(id);
    }

    public record StatusRequest(OrderStatus status) {}
}
