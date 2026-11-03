package org.group1.coffeeshopapi.inventory.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.constant.AppConstant;
import org.group1.coffeeshopapi.common.response.ApiResponse;
import org.group1.coffeeshopapi.common.response.PageResponse;
import org.group1.coffeeshopapi.common.security.CurrentActor;
import org.group1.coffeeshopapi.common.util.FileResponseUtil;
import org.group1.coffeeshopapi.common.util.PageUtil;
import org.group1.coffeeshopapi.common.enums.ReportPeriod;
import org.group1.coffeeshopapi.common.enums.StockMovementReason;
import org.group1.coffeeshopapi.common.enums.StockMovementType;
import org.group1.coffeeshopapi.inventory.dto.request.MovementSearch;
import org.group1.coffeeshopapi.inventory.dto.request.StockCountRequest;
import org.group1.coffeeshopapi.inventory.dto.request.StockCutRequest;
import org.group1.coffeeshopapi.inventory.dto.request.StockInRequest;
import org.group1.coffeeshopapi.inventory.dto.request.StockReturnRequest;
import org.group1.coffeeshopapi.inventory.dto.request.WasteRequest;
import org.group1.coffeeshopapi.inventory.dto.response.InventoryReportResponse;
import org.group1.coffeeshopapi.inventory.dto.response.InventoryResponse;
import org.group1.coffeeshopapi.inventory.dto.response.StockCutResponse;
import org.group1.coffeeshopapi.inventory.dto.response.StockInImportResponse;
import org.group1.coffeeshopapi.inventory.dto.response.StockMovementResponse;
import org.group1.coffeeshopapi.inventory.service.InventoryReportService;
import org.group1.coffeeshopapi.inventory.service.InventoryService;
import org.group1.coffeeshopapi.inventory.service.StockExpenseReportService;
import org.group1.coffeeshopapi.inventory.service.StockInImportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Admin and Barista: view stock levels, low-stock report and stock movement history "
        + "(read-only for Barista). Admin only: stock-in, stock-cut, waste, returns, stock counts and reports. "
        + "Quantities are in the product's sell unit unless a request says unit=PURCHASE.")
@SecurityRequirement(name = "bearerAuth")
public class InventoryController {

    private final InventoryService inventoryService;
    private final InventoryReportService inventoryReportService;
    private final StockExpenseReportService stockExpenseReportService;
    private final StockInImportService stockInImportService;
    private final CurrentActor currentActor;

    @GetMapping
    public ApiResponse<PageResponse<InventoryResponse>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                PageResponse.of(inventoryService.list(PageUtil.buildPageable(page, size))));
    }

    @GetMapping("/low-stock")
    public ApiResponse<PageResponse<InventoryResponse>> listLowStock(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                PageResponse.of(inventoryService.listLowStock(PageUtil.buildPageable(page, size))));
    }

    @GetMapping("/movements")
    public ApiResponse<PageResponse<StockMovementResponse>> searchMovements(
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) StockMovementType type,
            @RequestParam(required = false) StockMovementReason reason,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                PageResponse.of(inventoryService.searchMovements(new MovementSearch(productId, type, reason, from, to),
                        PageUtil.buildPageable(page, size, "createdAt", "desc"))));
    }

    @GetMapping("/reports/summary")
    public ApiResponse<InventoryReportResponse> reportSummary(
            @RequestParam(defaultValue = "DAY") ReportPeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                inventoryReportService.summary(period, date != null ? date : LocalDate.now()));
    }

    @GetMapping("/reports/summary/export")
    public ResponseEntity<byte[]> exportReportSummary(
            @RequestParam(defaultValue = "DAY") ReportPeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate reportDate = date != null ? date : LocalDate.now();
        return FileResponseUtil.respond(inventoryReportService.exportSummary(period, reportDate), FileResponseUtil.XLSX,
                "inventory-" + period.name().toLowerCase() + "-" + period.start(reportDate) + ".xlsx", false);
    }

    @GetMapping("/{productId}")
    public ApiResponse<InventoryResponse> getByProduct(@PathVariable UUID productId) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, inventoryService.getByProduct(productId));
    }

    @GetMapping("/{productId}/movements")
    public ApiResponse<PageResponse<StockMovementResponse>> listMovements(
            @PathVariable UUID productId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                PageResponse.of(inventoryService.listMovements(productId, PageUtil.buildPageable(page, size))));
    }

    @PostMapping("/stock-in")
    public ApiResponse<StockMovementResponse> stockIn(@Valid @RequestBody StockInRequest request) {
        StockMovementResponse response = inventoryService.stockIn(request, currentActor.id());
        return ApiResponse.of(HttpStatus.OK, "Stock received successfully.", response);
    }

    @GetMapping("/stock-in/import/template")
    public ResponseEntity<byte[]> downloadStockInTemplate() {
        return FileResponseUtil.respond(stockInImportService.generateTemplate(), FileResponseUtil.XLSX,
                "stock-in-import-template.xlsx", false);
    }

    @PostMapping(value = "/stock-in/import", consumes = "multipart/form-data")
    public ApiResponse<StockInImportResponse> stockInFromExcel(@RequestParam("file") MultipartFile file) {
        StockInImportResponse response = stockInImportService.importFromExcel(file, currentActor.id());
        return ApiResponse.of(HttpStatus.OK, "Import completed.", response);
    }

    @PostMapping("/stock-cut")
    public ApiResponse<StockCutResponse> stockCut(@Valid @RequestBody StockCutRequest request) {
        StockCutResponse response = inventoryService.stockCut(request, currentActor.id());
        return ApiResponse.of(HttpStatus.OK, "Stock cut successfully.", response);
    }

    @PostMapping("/waste")
    public ApiResponse<StockCutResponse> recordWaste(@Valid @RequestBody WasteRequest request) {
        return ApiResponse.of(HttpStatus.OK, "Waste recorded.", inventoryService.recordWaste(request, currentActor.id()));
    }

    @PostMapping("/returns")
    public ApiResponse<StockMovementResponse> returnToStock(@Valid @RequestBody StockReturnRequest request) {
        return ApiResponse.of(HttpStatus.OK, "Returned to stock.",
                inventoryService.returnToStock(request, currentActor.id()));
    }

    @PostMapping("/count")
    public ApiResponse<StockMovementResponse> adjustToCount(@Valid @RequestBody StockCountRequest request) {
        return ApiResponse.of(HttpStatus.OK, "Stock count saved.",
                inventoryService.adjustToCount(request, currentActor.id()));
    }

    @GetMapping("/expenses/report/monthly")
    public ResponseEntity<byte[]> monthlyExpenseReport(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        YearMonth reportMonth = month != null ? month : YearMonth.now();
        byte[] report = stockExpenseReportService.generateMonthlyReport(reportMonth);
        return FileResponseUtil.respond(report, FileResponseUtil.XLSX,
                "stock-expenses-" + reportMonth + ".xlsx", false);
    }
}
