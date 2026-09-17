package org.group1.coffeeshopapi.common.security;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.admin.entity.Admin;
import org.group1.coffeeshopapi.admin.repository.AdminRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

// Resolves the id of whoever is making the current request — a real Admin/Barista/Customer, or
// the Super Admin, which has no database row of its own. Use this on any endpoint the Super Admin
// can reach, instead of binding straight to CustomUserDetails (which would be null for them).
@Component
@RequiredArgsConstructor
public class CurrentActor {

    private final AdminRepository adminRepository;

    public UUID id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getId();
        }
        if (principal instanceof SuperAdminUserDetails) {
            return SuperAdminUserDetails.ID;
        }
        throw new IllegalStateException("Unsupported principal type: " + principal.getClass());
    }

    // A reference to the acting Admin, for entities that track "which admin did this" — null for
    // the Super Admin. Only call this on an Admin/Super-Admin-only endpoint.
    public Admin adminRef() {
        return adminRepository.referenceOrNull(id());
    }
}
