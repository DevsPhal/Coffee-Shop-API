package org.group1.coffeeshopapi.service;


import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.dto.response.UserResponse;
import org.group1.coffeeshopapi.entity.User;
import org.group1.coffeeshopapi.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getCurrentUser() {
        String email =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );
        return mapToResponse(user);
    }

    public UserResponse getUserById(UUID id) {
        User user =
                userRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"
                                )
                        );
        return mapToResponse(user);

    }

    public List<UserResponse> getAllUsers(){
        return userRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public void deleteUser(UUID id){

        User user =
                userRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"
                                )
                        );
        userRepository.delete(user);
    }

    private UserResponse mapToResponse(User user){
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}