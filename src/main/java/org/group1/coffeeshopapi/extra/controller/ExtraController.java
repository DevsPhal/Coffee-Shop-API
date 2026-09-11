package org.group1.coffeeshopapi.extra.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.constant.AppConstant;
import org.group1.coffeeshopapi.common.response.ApiResponse;
import org.group1.coffeeshopapi.extra.dto.request.CreateExtraRequest;
import org.group1.coffeeshopapi.extra.dto.request.UpdateExtraRequest;
import org.group1.coffeeshopapi.extra.dto.response.ExtraResponse;
import org.group1.coffeeshopapi.extra.service.ExtraService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/extras")
@RequiredArgsConstructor
@Tag(name = "Extras", description = "Admin only: manage the global add-on catalog (e.g. Pearl) — see Product Extras to offer one on a product")
@SecurityRequirement(name = "bearerAuth")
public class ExtraController {

    private final ExtraService extraService;

    @PostMapping
    public ResponseEntity<ApiResponse<ExtraResponse>> create(@Valid @RequestBody CreateExtraRequest request) {
        ExtraResponse response = extraService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(HttpStatus.CREATED, "Extra created successfully.", response));
    }

    @GetMapping
    public ApiResponse<List<ExtraResponse>> list() {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, extraService.list());
    }

    @PatchMapping("/{id}")
    public ApiResponse<ExtraResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateExtraRequest request) {
        return ApiResponse.of(HttpStatus.OK, "Extra updated successfully.", extraService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        extraService.delete(id);
        return ApiResponse.of(HttpStatus.OK, "Extra deleted successfully.", null);
    }
}
