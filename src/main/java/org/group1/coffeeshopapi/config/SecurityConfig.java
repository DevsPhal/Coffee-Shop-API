package org.group1.coffeeshopapi.config;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.constant.SecurityConstants;
import org.group1.coffeeshopapi.common.filter.JwtAuthFilter;
import org.group1.coffeeshopapi.common.security.RestAccessDeniedHandler;
import org.group1.coffeeshopapi.common.security.RestAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
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

    /**
     * Browser origins allowed to call this API. The storefront and admin are separate apps on
     * their own ports, so every call they make is cross-origin and needs these headers —
     * without them the browser blocks the request before it is sent and the client sees a
     * network error rather than an HTTP status. Override with CORS_ALLOWED_ORIGINS (comma
     * separated) when deploying behind real hostnames.
     */
    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:3001}")
    private List<String> allowedOrigins;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Explicit origins rather than "*": credentials are not used (the JWT travels in an
        // Authorization header), but naming the two apps keeps the surface honest.
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        // The clients read nothing custom off responses today; listed for the tracing header
        // the admin sends so it survives a stricter proxy later.
        config.setExposedHeaders(List.of("X-Request-ID"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
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
     * instead of failing cleanly. The {@code /api/admin/orders/**} endpoints the super admin does
     * reach this way (including its order-processing actions) use {@code CurrentActor} instead of
     * {@code @AuthenticationPrincipal CustomUserDetails}, which resolves the super admin to its
     * fixed id rather than NPE-ing — see {@code CurrentActor}'s javadoc.
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
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        // Preflights carry no Authorization header, so they must be allowed
                        // through before any rule below can reject them for being anonymous.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(SecurityConstants.PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events", "/api/shop/settings").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/contact-messages").permitAll()
                        .requestMatchers("/api/admin/contact-messages/**").hasRole("ADMIN")
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
                        .requestMatchers("/api/admin/attendance/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/orders/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/reports/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/banners/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/expenses/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/finance/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/bakong/**").hasRole("ADMIN")
                        .requestMatchers("/api/barista/orders/**").hasRole("BARISTA")
                        .requestMatchers(HttpMethod.GET, "/api/barista/reports/**").hasRole("BARISTA")
                        .requestMatchers("/api/barista/attendance/**").hasRole("BARISTA")
                        // New staff resources require an explicit permission rule above.
                        .requestMatchers("/api/admin/**", "/api/barista/**").denyAll()
                        // The storefront menu is readable without an account — a visitor has to
                        // be able to see what is for sale before deciding to register. Only the
                        // GETs open up; the cart, orders and every write below still require a
                        // signed-in customer, and this controller exposes no principal-dependent
                        // data (no prices per user, no stock levels).
                        .requestMatchers(HttpMethod.GET, "/api/customer/products/**").permitAll()
                        .requestMatchers("/api/customer/**").hasRole("CUSTOMER")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
