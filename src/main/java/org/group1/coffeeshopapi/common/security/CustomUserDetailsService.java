package org.group1.coffeeshopapi.common.security;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.properties.SuperAdminProperties;
import org.group1.coffeeshopapi.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    private final SuperAdminProperties superAdminProperties;
    private final PasswordEncoder passwordEncoder;

    private volatile String cachedSuperAdminPasswordHash;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        if (superAdminProperties.matches(email)) {
            return new SuperAdminUserDetails(superAdminProperties.getEmail(), superAdminPasswordHash());
        }
        return userRepository.findByEmail(email)
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("No account found for email: " + email));
    }

    /**
     * BCrypt is deliberately slow, so the hash of the configured super admin password is
     * computed once and cached rather than re-hashed on every login attempt.
     */
    private String superAdminPasswordHash() {
        String hash = cachedSuperAdminPasswordHash;
        if (hash == null) {
            synchronized (this) {
                hash = cachedSuperAdminPasswordHash;
                if (hash == null) {
                    hash = passwordEncoder.encode(superAdminProperties.getPassword());
                    cachedSuperAdminPasswordHash = hash;
                }
            }
        }
        return hash;
    }
}