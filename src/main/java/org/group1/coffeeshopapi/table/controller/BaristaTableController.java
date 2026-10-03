package org.group1.coffeeshopapi.table.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.constant.AppConstant;
import org.group1.coffeeshopapi.common.enums.TableStatus;
import org.group1.coffeeshopapi.common.response.ApiResponse;
import org.group1.coffeeshopapi.table.dto.request.UpdateTableStatusRequest;
import org.group1.coffeeshopapi.table.dto.response.TableActivityResponse;
import org.group1.coffeeshopapi.table.dto.response.TableResponse;
import org.group1.coffeeshopapi.table.service.DiningTableService;
import org.group1.coffeeshopapi.table.service.TableActivityService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/barista/tables")
@RequiredArgsConstructor
@Tag(name = "Barista Tables", description = "Barista only: see live table activity and seat or free guests")
@SecurityRequirement(name = "bearerAuth")
public class BaristaTableController {

    private final DiningTableService diningTableService;
    private final TableActivityService tableActivityService;

    @GetMapping
    public ApiResponse<List<TableActivityResponse>> list(@RequestParam(required = false) TableStatus status) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, tableActivityService.list(status));
    }

    @GetMapping("/{id}")
    public ApiResponse<TableActivityResponse> getById(@PathVariable UUID id) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, tableActivityService.getById(id));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<TableResponse> updateStatus(
            @PathVariable UUID id, @Valid @RequestBody UpdateTableStatusRequest request) {
        return ApiResponse.of(HttpStatus.OK, "Table status updated successfully.",
                diningTableService.updateStatus(id, request));
    }
}
