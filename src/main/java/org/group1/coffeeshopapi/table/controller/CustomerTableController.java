package org.group1.coffeeshopapi.table.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.constant.AppConstant;
import org.group1.coffeeshopapi.common.response.ApiResponse;
import org.group1.coffeeshopapi.common.security.CustomUserDetails;
import org.group1.coffeeshopapi.order.dto.response.OrderResponse;
import org.group1.coffeeshopapi.order.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customer/tables")
@RequiredArgsConstructor
@Tag(name = "Customer Tables", description = "Customer only: track own active dine-in orders at a table")
@SecurityRequirement(name = "bearerAuth")
public class CustomerTableController {

    private final OrderService orderService;

    @GetMapping("/{tableNumber}/orders")
    public ApiResponse<List<OrderResponse>> listActiveOrders(
            @PathVariable String tableNumber, @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                orderService.listActiveAtTableForCustomer(tableNumber, currentUser.getId()));
    }
}
