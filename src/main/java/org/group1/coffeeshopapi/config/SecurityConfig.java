package org.group1.coffeeshopapi.config;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.constant.SecurityConstants;
import org.group1.coffeeshopapi.common.filter.JwtAuthFilter;
import org.group1.coffeeshopapi.common.security.RestAccessDeniedHandler;
import org.group1.coffeeshopapi.common.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * Grants SUPER_ADMIN every ADMIN-scoped authority, so it satisfies any hasRole("ADMIN")
     * check in this filter chain — including ones added later — without having to remember to
     * add it to each new requestMatcher individually.
     * <p>
     * Deliberately NOT extended to BARISTA/CUSTOMER: those endpoints key off a real
     * {@code baristaId}/{@code customerId} row (POS sales, self-service carts) and every
     * controller there injects {@code @AuthenticationPrincipal CustomUserDetails} unconditionally.
     * The super admin's principal is {@code SuperAdminUserDetails}, a different type with no
     * backing row — letting it past the role check would just NPE on {@code currentUser.getId()}
     * instead of failing cleanly. Admin-wide visibility into all orders is already available via
     * the {@code /api/admin/orders/**} endpoints, which don't have this problem.
     */
    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role("SUPER_ADMIN").implies("ADMIN")
                .build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SecurityConstants.PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers("/api/admin/admins/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/admin/users/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/admin/baristas/**").hasRole("ADMIN")
                        // Baristas need read-only access to the catalog to ring up sales, and to
                        // stock levels/movements/low-stock so they can see what an order deducted
                        // and flag what needs restocking — without being able to adjust it
                        // themselves (stock-in/stock-cut stay Admin-only, matched below).
                        .requestMatchers(HttpMethod.GET, "/api/admin/categories/**", "/api/admin/products/**",
                                "/api/admin/inventory/**")
                        .hasAnyRole("ADMIN", "BARISTA")
                        .requestMatchers("/api/admin/categories/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/products/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/inventory/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/events/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/orders/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/reports/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/banners/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/expenses/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/finance/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/bakong/**").hasRole("ADMIN")
                        .requestMatchers("/api/barista/orders/**").hasRole("BARISTA")
                        .requestMatchers("/api/barista/reports/**").hasRole("BARISTA")
                        .requestMatchers("/api/customer/**").hasRole("CUSTOMER")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}