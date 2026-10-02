package org.group1.coffeeshopapi.table.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.constant.AppConstant;
import org.group1.coffeeshopapi.common.enums.TableStatus;
import org.group1.coffeeshopapi.common.response.ApiResponse;
import org.group1.coffeeshopapi.common.response.PageResponse;
import org.group1.coffeeshopapi.common.util.FileResponseUtil;
import org.group1.coffeeshopapi.common.util.PageUtil;
import org.group1.coffeeshopapi.common.util.QrImageUtil;
import org.group1.coffeeshopapi.table.dto.request.CreateTableRequest;
import org.group1.coffeeshopapi.table.dto.request.UpdateTableRequest;
import org.group1.coffeeshopapi.table.dto.request.UpdateTableStatusRequest;
import org.group1.coffeeshopapi.table.dto.response.TableResponse;
import org.group1.coffeeshopapi.table.service.DiningTableService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/tables")
@RequiredArgsConstructor
@Tag(name = "Admin Tables", description = "Admin only: manage dine-in tables, seat guests and print table QR codes")
@SecurityRequirement(name = "bearerAuth")
public class AdminTableController {

    private static final int QR_IMAGE_SIZE = 512;

    private final DiningTableService diningTableService;

    @PostMapping
    public ResponseEntity<ApiResponse<TableResponse>> create(@Valid @RequestBody CreateTableRequest request) {
        TableResponse response = diningTableService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(HttpStatus.CREATED, "Table created successfully.", response));
    }

    @GetMapping
    public ApiResponse<PageResponse<TableResponse>> list(
            @RequestParam(required = false) TableStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                PageResponse.of(diningTableService.list(status, PageUtil.buildPageable(page, size))));
    }

    @GetMapping("/{id}")
    public ApiResponse<TableResponse> getById(@PathVariable UUID id) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, diningTableService.getById(id));
    }

    @PatchMapping("/{id}")
    public ApiResponse<TableResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateTableRequest request) {
        return ApiResponse.of(HttpStatus.OK, "Table updated successfully.", diningTableService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<TableResponse> updateStatus(
            @PathVariable UUID id, @Valid @RequestBody UpdateTableStatusRequest request) {
        return ApiResponse.of(HttpStatus.OK, "Table status updated successfully.",
                diningTableService.updateStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        diningTableService.delete(id);
        return ApiResponse.of(HttpStatus.OK, "Table deleted successfully.", null);
    }

    @GetMapping(value = "/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getQrImage(@PathVariable UUID id) {
        TableResponse table = diningTableService.getById(id);
        byte[] png = QrImageUtil.toPng(table.scanUrl(), QR_IMAGE_SIZE);
        return FileResponseUtil.respond(png, MediaType.IMAGE_PNG, "table-" + table.tableNumber() + "-qr.png", true);
    }
}
