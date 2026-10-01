package org.group1.coffeeshopapi.admin.service;

import org.group1.coffeeshopapi.admin.dto.request.CreateStaffRequest;
import org.group1.coffeeshopapi.admin.dto.request.InviteStaffRequest;
import org.group1.coffeeshopapi.admin.dto.request.UpdateStaffRequest;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.telegram.dto.TelegramLinkCodeResponse;
import org.group1.coffeeshopapi.user.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface StaffService {
    UserResponse create(CreateStaffRequest request, Role role, UUID createdBy);

    TelegramLinkCodeResponse createViaTelegram(InviteStaffRequest request, Role role, UUID createdBy);

    TelegramLinkCodeResponse resendTelegramInvite(UUID id, Role role);

    UserResponse getById(UUID id, Role role);
    Page<UserResponse> list(Role role, Pageable pageable);
    UserResponse update(UUID id, UpdateStaffRequest request, Role role);
    UserResponse uploadAvatar(UUID id, MultipartFile file, Role role);
    void delete(UUID id, Role role);
}
