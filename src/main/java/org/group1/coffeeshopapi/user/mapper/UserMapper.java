package org.group1.coffeeshopapi.user.mapper;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.admin.entity.Admin;
import org.group1.coffeeshopapi.barista.entity.Barista;
import org.group1.coffeeshopapi.user.dto.response.ActorSummary;
import org.group1.coffeeshopapi.user.dto.response.UserResponse;
import org.group1.coffeeshopapi.user.entity.Customer;
import org.group1.coffeeshopapi.user.entity.User;
import org.group1.coffeeshopapi.user.service.ActorLookupService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final ActorLookupService actorLookupService;

    public UserResponse toResponse(User user) {
        // Telegram linking is a customer-only concern — Admin/Barista never have a chat linked.
        boolean telegramLinked = user instanceof Customer customer && customer.getTelegramChatId() != null;

        // Only ADMIN/BARISTA rows carry a creator — resolved lazily so fetching a customer never
        // pays for a lookup that will always come back null.
        UUID createdBy = switch (user) {
            case Admin admin -> admin.getCreatedBy();
            case Barista barista -> barista.getCreatedBy();
            default -> null;
        };
        ActorSummary createdByActor = actorLookupService.resolve(createdBy);

        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .gender(user.getGender())
                .role(user.getRole())
                .status(user.getStatus())
                .telegramLinked(telegramLinked)
                .createdBy(createdBy)
                .createdByName(createdByActor != null ? createdByActor.name() : null)
                .createdByRole(createdByActor != null ? createdByActor.role() : null)
                .build();
    }
}