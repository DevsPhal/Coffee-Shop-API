package org.group1.coffeeshopapi.admin.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.admin.dto.request.CreateStaffRequest;
import org.group1.coffeeshopapi.admin.dto.request.UpdateStaffRequest;
import org.group1.coffeeshopapi.admin.service.StaffService;
import org.group1.coffeeshopapi.common.constant.AppConstant;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.response.ApiResponse;
import org.group1.coffeeshopapi.common.response.PageResponse;
import org.group1.coffeeshopapi.common.util.PageUtil;
import org.group1.coffeeshopapi.user.dto.response.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/admins")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Super admin only: full CRUD for admin accounts")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final StaffService staffService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody CreateStaffRequest request) {
        UserResponse admin = staffService.create(request, Role.ADMIN);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(HttpStatus.CREATED, "Admin account created successfully.", admin));
    }

    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                PageResponse.of(staffService.list(Role.ADMIN, PageUtil.buildPageable(page, size))));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getById(@PathVariable UUID id) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, staffService.getById(id, Role.ADMIN));
    }

    @PatchMapping("/{id}")
    public ApiResponse<UserResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateStaffRequest request) {
        UserResponse admin = staffService.update(id, request, Role.ADMIN);
        return ApiResponse.of(HttpStatus.OK, "Admin account updated successfully.", admin);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        staffService.delete(id, Role.ADMIN);
        return ApiResponse.of(HttpStatus.OK, "Admin account deleted successfully.", null);
    }
}