package org.group1.coffeeshopapi.common.security;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.admin.entity.Admin;
import org.group1.coffeeshopapi.admin.repository.AdminRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

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

    public Admin adminRef() {
        return adminRepository.referenceOrNull(id());
    }
}
