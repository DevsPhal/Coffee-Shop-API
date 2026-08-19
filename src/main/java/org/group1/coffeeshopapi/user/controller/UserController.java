package org.group1.coffeeshopapi.user.controller;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.constant.AppConstant;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.response.ApiResponse;
import org.group1.coffeeshopapi.common.security.CustomUserDetails;
import org.group1.coffeeshopapi.common.security.SuperAdminUserDetails;
import org.group1.coffeeshopapi.telegram.dto.TelegramLinkCodeResponse;
import org.group1.coffeeshopapi.telegram.service.TelegramLinkService;
import org.group1.coffeeshopapi.user.dto.response.SuperAdminResponse;
import org.group1.coffeeshopapi.user.dto.response.UserResponse;
import org.group1.coffeeshopapi.user.mapper.UserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Authenticated user profile and Telegram linking")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserMapper userMapper;
    private final TelegramLinkService telegramLinkService;

    @GetMapping("/me")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
            description = "data is a UserResponse for normal accounts, or a SuperAdminResponse for the super admin",
            content = @Content(schema = @Schema(oneOf = {UserResponse.class, SuperAdminResponse.class})))
    public ApiResponse<Object> me(@AuthenticationPrincipal UserDetails principal) {
        Object profile = principal instanceof CustomUserDetails customUserDetails
                ? userMapper.toResponse(customUserDetails.getUser())
                : ((SuperAdminUserDetails) principal).toResponse();
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, profile);
    }

    @PostMapping("/me/telegram/link-code")
    public ApiResponse<TelegramLinkCodeResponse> createTelegramLinkCode(@AuthenticationPrincipal UserDetails principal) {
        if (!(principal instanceof CustomUserDetails customUserDetails) || customUserDetails.getRole() != Role.CUSTOMER) {
            throw new InvalidOperationException("Telegram linking is only available for customer accounts");
        }
        TelegramLinkCodeResponse linkCode = telegramLinkService.generateLinkCode(customUserDetails.getId());
        return ApiResponse.of(HttpStatus.OK, "Telegram link code generated.", linkCode);
    }
}