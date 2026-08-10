package org.group1.coffeeshopapi.admin.controller;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.responses.ApiResponse;
import org.group1.coffeeshopapi.auth.dto.response.UserResponse;
import org.group1.coffeeshopapi.admin.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/user")
@RequiredArgsConstructor
public class UserAdminController {
    private final UserService userService;

    @GetMapping("/me")
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
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers(){
        List<UserResponse> getAllUsers = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.<List<UserResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Get all users successfully.")
                .data(getAllUsers)
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/{uId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long uId){
        UserResponse response = userService.getUserById(uId);
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Get successfully user with ID: " + uId)
                .data(response)
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteById(@PathVariable Long id){
        userService.delete(id);
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Delete user successfully.")
                .data(null)
                .build();
        return ResponseEntity.ok(response);
    }
}
