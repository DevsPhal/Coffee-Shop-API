package org.group1.coffeeshopapi.user.mapper;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.admin.entity.Admin;
import org.group1.coffeeshopapi.barista.entity.Barista;
import org.group1.coffeeshopapi.common.enums.RegisterType;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.user.dto.response.ActorSummary;
import org.group1.coffeeshopapi.user.dto.response.UserResponse;
import org.group1.coffeeshopapi.user.entity.User;
import org.group1.coffeeshopapi.user.service.ActorLookupService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserMapper {

    // Admin.createdBy stays a plain audit id rather than a relation (see its own javadoc: only
    // the Super Admin can create another Admin, so a real FK there would always be null anyway),
    // so it's still resolved via ActorLookupService. Barista.createdByAdmin is a real relation —
    // see Barista's javadoc for why it's still nullable (the Super Admin case).
    private final ActorLookupService actorLookupService;

    public UserResponse toResponse(User user) {
        // telegramChatId lives on the shared User entity — an Admin/Barista invited via Telegram
        // (see StaffServiceImpl#createViaTelegram) has one linked too, not just customers.
        boolean telegramLinked = user.getTelegramChatId() != null;

        UUID createdBy = null;
        String createdByName = null;
        Role createdByRole = null;
        if (user instanceof Admin admin) {
            createdBy = admin.getCreatedBy();
            ActorSummary createdByActor = actorLookupService.resolve(createdBy);
            if (createdByActor != null) {
                createdByName = createdByActor.name();
                createdByRole = createdByActor.role();
            }
        } else if (user instanceof Barista barista && barista.getCreatedByAdmin() != null) {
            Admin creator = barista.getCreatedByAdmin();
            createdBy = creator.getId();
            createdByName = creator.getFullName();
            createdByRole = Role.ADMIN;
        }

        // A Telegram-invited staff account (see StaffServiceImpl#buildInvitedStaff) has no real
        // email — the column just holds an internal, never-seen placeholder — so it's hidden here
        // rather than shown as a nonsense address in the admin UI.
        String email = user.getRegisterType() == RegisterType.TELEGRAM ? null : user.getEmail();

        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(email)
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .gender(user.getGender())
                .role(user.getRole())
                .status(user.getStatus())
                .telegramLinked(telegramLinked)
                .registerType(user.getRegisterType())
                .createdBy(createdBy)
                .createdByName(createdByName)
                .createdByRole(createdByRole)
                .build();
    }
}