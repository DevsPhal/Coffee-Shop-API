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

/**
 * Generic CRUD for staff accounts (ADMIN, BARISTA), scoped by role so a caller managing one
 * role can never read/modify/delete an account of a different role through the same endpoint.
 */
public interface StaffService {
    UserResponse create(CreateStaffRequest request, Role role, UUID createdBy);

    /**
     * Invite-only creation path: no email or password is collected — just a name and the phone
     * number the invitee is expected to share back from their own Telegram contact card. The
     * account sits at PENDING_VERIFICATION until they open the returned link, share that contact,
     * and it matches (see TelegramLinkServiceImpl#verifyPendingContact); it then goes ACTIVE
     * directly, with no separate OTP step. Login afterward is Telegram-widget-only
     * (AuthService#loginViaTelegramWidget) — unlike a customer linking Telegram, which is only ever
     * an alternate login for an account that already has real email/password credentials.
     */
    TelegramLinkCodeResponse createViaTelegram(InviteStaffRequest request, Role role, UUID createdBy);

    /**
     * Issues a fresh link code for a staff member still stuck at PENDING_VERIFICATION because
     * their original invite code (5-minute TTL — see TelegramProperties#linkCodeTtlSeconds)
     * expired before they opened it. Without this, the only recovery was deleting and recreating
     * the account under the same email.
     */
    TelegramLinkCodeResponse resendTelegramInvite(UUID id, Role role);

    UserResponse getById(UUID id, Role role);
    Page<UserResponse> list(Role role, Pageable pageable);
    UserResponse update(UUID id, UpdateStaffRequest request, Role role);
    UserResponse uploadAvatar(UUID id, MultipartFile file, Role role);
    void delete(UUID id, Role role);
}
