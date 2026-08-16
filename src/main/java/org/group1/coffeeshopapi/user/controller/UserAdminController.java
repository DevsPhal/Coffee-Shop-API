package org.group1.coffeeshopapi.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.admin.dto.request.UpdateUserRoleRequest;
import org.group1.coffeeshopapi.common.responses.PaginatedResponse;
import org.group1.coffeeshopapi.common.utils.PageUtil;
import org.group1.coffeeshopapi.user.dto.response.UserResponse;
import org.group1.coffeeshopapi.user.service.UserService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
public class UserAdminController {
    private final UserService userService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse me(){
        return userService.getCurrentUser();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PaginatedResponse<UserResponse> getAllUsers(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction){
        Pageable pageable = PageUtil.buildPageable(page, size, sortBy, direction);
        return userService.getAllUsers(pageable);
    }

    @GetMapping("/{uId}")
    @PreAuthorize("hasRole('ADMIN') or #uId == authentication.principal.id")
    public UserResponse getUserById(@PathVariable UUID uId){
        return userService.getUserById(uId);
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateRole(@PathVariable UUID id,
                                   @Valid @RequestBody UpdateUserRoleRequest request){
        return userService.updateRole(id, request.getRole());
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteById(@PathVariable UUID id){
        userService.delete(id);
    }
}
