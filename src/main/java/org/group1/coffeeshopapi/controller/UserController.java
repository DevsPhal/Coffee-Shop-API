package org.group1.coffeeshopapi.controller;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.dto.response.ApiResponse;
import org.group1.coffeeshopapi.dto.response.UserResponse;
import org.group1.coffeeshopapi.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
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
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID uId){
        UserResponse response = userService.getUserById(uId);
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Get successfully user with ID: " + uId)
                .data(response)
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/{uId}")
    public ResponseEntity<ApiResponse<Void>> deleteById(@PathVariable UUID uId){
        userService.deleteUserById(uId);
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Delete user successfully.")
                .data(null)
                .build();
        return ResponseEntity.ok(response);
    }
}
