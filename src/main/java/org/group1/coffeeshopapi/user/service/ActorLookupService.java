package org.group1.coffeeshopapi.user.service;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.security.SuperAdminUserDetails;
import org.group1.coffeeshopapi.user.dto.response.ActorSummary;
import org.group1.coffeeshopapi.user.entity.User;
import org.group1.coffeeshopapi.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// Turns an audit id into a displayable name + role. The Super Admin is special-cased since it
// has no backing row; everyone else is a normal user lookup.
@Service
@RequiredArgsConstructor
public class ActorLookupService {

    private static final String SUPER_ADMIN_DISPLAY_NAME = "Super Admin";
    private static final String DELETED_ACCOUNT_DISPLAY_NAME = "Deleted account";

    private final UserRepository userRepository;

    public ActorSummary resolve(UUID actorId) {
        if (actorId == null) {
            return null;
        }
        if (actorId.equals(SuperAdminUserDetails.ID)) {
            return new ActorSummary(actorId, SUPER_ADMIN_DISPLAY_NAME, Role.SUPER_ADMIN);
        }
        return userRepository.findById(actorId)
                .map(user -> toSummary(actorId, user))
                .orElseGet(() -> new ActorSummary(actorId, DELETED_ACCOUNT_DISPLAY_NAME, null));
    }

    // Batched form of resolve(), for list responses — avoids one query per row.
    public Map<UUID, ActorSummary> resolveAll(Collection<UUID> actorIds) {
        Set<UUID> distinctIds = new LinkedHashSet<>();
        for (UUID id : actorIds) {
            if (id != null) {
                distinctIds.add(id);
            }
        }

        Map<UUID, ActorSummary> resolved = new HashMap<>();
        Set<UUID> idsToLookUp = new LinkedHashSet<>();
        for (UUID id : distinctIds) {
            if (id.equals(SuperAdminUserDetails.ID)) {
                resolved.put(id, new ActorSummary(id, SUPER_ADMIN_DISPLAY_NAME, Role.SUPER_ADMIN));
            } else {
                idsToLookUp.add(id);
            }
        }

        if (!idsToLookUp.isEmpty()) {
            for (User user : userRepository.findAllById(idsToLookUp)) {
                resolved.put(user.getId(), toSummary(user.getId(), user));
            }
        }
        for (UUID id : idsToLookUp) {
            resolved.putIfAbsent(id, new ActorSummary(id, DELETED_ACCOUNT_DISPLAY_NAME, null));
        }

        return resolved;
    }

    private ActorSummary toSummary(UUID actorId, User user) {
        return new ActorSummary(actorId, user.getFullName(), user.getRole());
    }
}
