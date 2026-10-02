package org.group1.coffeeshopapi.table.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.constant.AppConstant;
import org.group1.coffeeshopapi.common.enums.TableStatus;
import org.group1.coffeeshopapi.common.response.ApiResponse;
import org.group1.coffeeshopapi.table.dto.response.TableResponse;
import org.group1.coffeeshopapi.table.service.DiningTableService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tables")
@RequiredArgsConstructor
@Tag(name = "Tables", description = "Public: look up a dine-in table by number (target of the table QR code)")
public class PublicTableController {

    private final DiningTableService diningTableService;

    @GetMapping
    public ApiResponse<List<TableResponse>> list(@RequestParam(required = false) TableStatus status) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, diningTableService.listPublic(status));
    }

    @GetMapping("/{tableNumber}")
    public ApiResponse<TableResponse> getByNumber(@PathVariable String tableNumber) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, diningTableService.getByNumber(tableNumber));
    }
}
