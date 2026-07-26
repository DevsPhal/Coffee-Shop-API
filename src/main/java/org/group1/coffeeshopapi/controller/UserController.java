package org.group1.coffeeshopapi.controller;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.dto.response.UserResponse;
import org.group1.coffeeshopapi.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(){
        return ResponseEntity.ok(userService.getCurrentUser());
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers(){
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{uId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID uId){
        return ResponseEntity.ok(userService.getUserById(uId));
    }

    @DeleteMapping("/{uId}")
    public ResponseEntity<Void> delete(@PathVariable UUID uId){
        userService.deleteUser(uId);
        return ResponseEntity.noContent().build();
    }
}
