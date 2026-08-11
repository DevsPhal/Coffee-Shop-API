package org.group1.coffeeshopapi.admin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.admin.dto.request.UpdateUserRoleRequest;
import org.group1.coffeeshopapi.common.responses.ApiResponse;
import org.group1.coffeeshopapi.common.responses.PageResponse;
import org.group1.coffeeshopapi.common.utils.PageUtil;
import org.group1.coffeeshopapi.auth.dto.response.UserResponse;
import org.group1.coffeeshopapi.admin.service.UserService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
public class UserAdminController {
    private final UserService userService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> me(){
        UserResponse response = userService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Get current user successfully.")
                .data(response)
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getAllUsers(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction){
        Pageable pageable = PageUtil.buildPageable(page, size, sortBy, direction);
        PageResponse<UserResponse> getAllUsers = userService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.<PageResponse<UserResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Get all users successfully.")
                .data(getAllUsers)
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/{uId}")
    @PreAuthorize("hasRole('ADMIN') or #uId == authentication.principal.id")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID uId){
        UserResponse response = userService.getUserById(uId);
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Get successfully user with ID: " + uId)
                .data(response)
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateRole(@PathVariable UUID id,
                                                                  @Valid @RequestBody UpdateUserRoleRequest request){
        UserResponse response = userService.updateRole(id, request.getRole());
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .status(HttpStatus.OK.value())
                .message("User role updated successfully.")
                .data(response)
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteById(@PathVariable UUID id){
        userService.delete(id);
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Delete user successfully.")
                .data(null)
                .timeStamp(LocalDateTime.now())
                .build();
        return ResponseEntity.ok(response);
    }
}
