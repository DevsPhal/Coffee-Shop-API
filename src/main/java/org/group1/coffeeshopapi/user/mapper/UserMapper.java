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
import org.hibernate.proxy.HibernateProxy;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final ActorLookupService actorLookupService;

    public UserResponse toResponse(User user) {
        boolean telegramLinked = user.getTelegramChatId() != null;

        UUID createdBy = null;
        String createdByName = null;
        Role createdByRole = null;
        if (user instanceof Admin admin) {
            createdBy = admin.getCreatedBy();
        } else if (user instanceof Barista barista) {
            createdBy = idOf(barista.getCreatedByAdmin());
        }
        // Resolved by id, never by walking the lazy creator relation: /me maps the user the JWT
        // filter loaded, whose session is already closed, so touching the creator's fields there
        // threw LazyInitializationException and failed /me for every admin-created barista.
        ActorSummary createdByActor = actorLookupService.resolve(createdBy);
        if (createdByActor != null) {
            createdByName = createdByActor.name();
            createdByRole = createdByActor.role();
        }

        // A Telegram-invited account has no real email — just an internal placeholder — so hide it.
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
                .telegramUsername(user.getTelegramUsername())
                .registerType(user.getRegisterType())
                .createdBy(createdBy)
                .createdByName(createdByName)
                .createdByRole(createdByRole)
                .build();
    }

    // A lazy proxy knows its id without loading the row (and without an open session).
    private static UUID idOf(Admin admin) {
        if (admin == null) {
            return null;
        }
        return admin instanceof HibernateProxy proxy
                ? (UUID) proxy.getHibernateLazyInitializer().getIdentifier()
                : admin.getId();
    }
}
