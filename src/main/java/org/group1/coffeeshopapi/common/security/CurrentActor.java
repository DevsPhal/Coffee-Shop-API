package org.group1.coffeeshopapi.common.security;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.admin.entity.Admin;
import org.group1.coffeeshopapi.admin.repository.AdminRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Resolves the id of whoever is making the current request, regardless of whether the principal
 * is a real Admin/Barista/Customer row ({@link CustomUserDetails}) or the config-driven Super
 * Admin ({@link SuperAdminUserDetails}), which has no backing row and always resolves to the same
 * reserved id. Use this instead of {@code @AuthenticationPrincipal CustomUserDetails} on any
 * endpoint the Super Admin can reach (every {@code hasRole("ADMIN")} route, via the role
 * hierarchy) — binding straight to {@code CustomUserDetails} silently resolves to {@code null}
 * for the Super Admin and NPEs the moment {@code .getId()} is called.
 */
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

    // A lazy reference to the acting Admin, for entities that track "which admin did this" as a
    // real @ManyToOne relation — null when the actor is the Super Admin (see
    // AdminRepository.referenceOrNull). Only call this on an endpoint that's actually
    // ADMIN/SUPER_ADMIN-only — a Barista's id isn't in "admins" either and getReferenceById won't
    // fail until the proxy is touched, so misuse here fails far from the actual bug.
    public Admin adminRef() {
        return adminRepository.referenceOrNull(id());
    }
}
