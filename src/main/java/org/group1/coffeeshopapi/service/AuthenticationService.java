package org.group1.coffeeshopapi.service;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.dto.request.LoginRequest;
import org.group1.coffeeshopapi.dto.request.RegisterRequest;
import org.group1.coffeeshopapi.dto.response.AuthenticationResponse;
import org.group1.coffeeshopapi.entity.Role;
import org.group1.coffeeshopapi.entity.User;
import org.group1.coffeeshopapi.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.repository.UserRepository;
import org.group1.coffeeshopapi.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    private final AuthenticationManager authenticationManager;

    public AuthenticationResponse register(RegisterRequest request) {

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(
                        passwordEncoder.encode(
                                request.getPassword()
                        )
                )
                .role(Role.USER)
                .build();
        userRepository.save(user);

        String token = jwtService.generateToken(
                createUserDetails(user)
        );
        return AuthenticationResponse.builder()
                .token(token)
                .build();
    }

    public AuthenticationResponse login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(
                        request.getEmail()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );

        String token = jwtService.generateToken(
                createUserDetails(user)
        );

        return AuthenticationResponse.builder()

                .token(token)

                .build();

    }

    private org.springframework.security.core.userdetails.User
    createUserDetails(User user) {
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                List.of(
                        new SimpleGrantedAuthority(
                                "ROLE_" + user.getRole().name()
                        )
                )
        );
    }
}