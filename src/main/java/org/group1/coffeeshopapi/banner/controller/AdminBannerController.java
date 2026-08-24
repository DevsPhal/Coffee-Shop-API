package org.group1.coffeeshopapi.banner.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.banner.dto.request.CreateBannerRequest;
import org.group1.coffeeshopapi.banner.dto.request.UpdateBannerRequest;
import org.group1.coffeeshopapi.banner.dto.response.BannerResponse;
import org.group1.coffeeshopapi.banner.service.BannerService;
import org.group1.coffeeshopapi.common.constant.AppConstant;
import org.group1.coffeeshopapi.common.response.ApiResponse;
import org.group1.coffeeshopapi.common.response.PageResponse;
import org.group1.coffeeshopapi.common.security.CurrentActor;
import org.group1.coffeeshopapi.common.util.PageUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/banners")
@RequiredArgsConstructor
@Tag(name = "Admin Banners", description = "Admin only: manage landing page banners")
@SecurityRequirement(name = "bearerAuth")
public class AdminBannerController {

    private final BannerService bannerService;
    private final CurrentActor currentActor;

    @PostMapping
    public ResponseEntity<ApiResponse<BannerResponse>> create(@Valid @RequestBody CreateBannerRequest request) {
        BannerResponse response = bannerService.create(request, currentActor.id());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(HttpStatus.CREATED, "Banner created successfully.", response));
    }

    @GetMapping
    public ApiResponse<PageResponse<BannerResponse>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                PageResponse.of(bannerService.list(PageUtil.buildPageable(page, size))));
    }

    @GetMapping("/{id}")
    public ApiResponse<BannerResponse> getById(@PathVariable UUID id) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, bannerService.getById(id));
    }

    @PatchMapping("/{id}")
    public ApiResponse<BannerResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateBannerRequest request) {
        return ApiResponse.of(HttpStatus.OK, "Banner updated successfully.",
                bannerService.update(id, request, currentActor.id()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        bannerService.delete(id);
        return ApiResponse.of(HttpStatus.OK, "Banner deleted successfully.", null);
    }

    @PostMapping(value = "/{id}/image", consumes = "multipart/form-data")
    public ApiResponse<BannerResponse> uploadImage(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return ApiResponse.of(HttpStatus.OK, "Banner image uploaded successfully.",
                bannerService.uploadImage(id, file, currentActor.id()));
    }

    @DeleteMapping("/{id}/image")
    public ApiResponse<BannerResponse> removeImage(@PathVariable UUID id) {
        return ApiResponse.of(HttpStatus.OK, "Banner image removed successfully.",
                bannerService.removeImage(id, currentActor.id()));
    }
}
