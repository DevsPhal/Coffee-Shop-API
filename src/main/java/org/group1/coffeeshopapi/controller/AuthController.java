package org.group1.coffeeshopapi.controller;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.dto.request.LoginRequest;
import org.group1.coffeeshopapi.dto.request.RegisterRequest;
import org.group1.coffeeshopapi.dto.response.AuthenticationResponse;
import org.group1.coffeeshopapi.dto.response.UserResponse;
import org.group1.coffeeshopapi.service.AuthenticationService;
import org.group1.coffeeshopapi.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;

    private final UserService userService;

    @PostMapping("/register")
    public AuthenticationResponse register(
            @RequestBody RegisterRequest request
    ){

        return authenticationService.register(request);

    }

    @PostMapping("/login")
    public AuthenticationResponse login(
            @RequestBody LoginRequest request
    ){

        return authenticationService.login(request);

    }

    @GetMapping("/hello")
    public String hello(){

        return "Security works!";

    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(){


        return ResponseEntity.ok(
                userService.getCurrentUser()
        );

    }

}