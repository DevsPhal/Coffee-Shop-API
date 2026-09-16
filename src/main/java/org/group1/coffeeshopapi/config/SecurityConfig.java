package org.group1.coffeeshopapi.config;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.constant.SecurityConstants;
import org.group1.coffeeshopapi.common.filter.JwtAuthFilter;
import org.group1.coffeeshopapi.common.properties.CorsProperties;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;
    private final CorsProperties corsProperties;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    // Without this, the browser blocks every request a real frontend (on its own origin) makes to
    // this API — registration, login, everything — since Spring Security answers with no
    // Access-Control-Allow-* headers at all otherwise. A tool like curl/Postman never hits this
    // (CORS is a browser-only restriction), which is exactly why this can look "broken only in
    // production": a local dev frontend often proxies /api same-origin, masking the gap that a
    // real deployed frontend then walks straight into. See CorsProperties/CORS_ALLOWED_ORIGINS.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

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
                        .requestMatchers(SecurityConstants.PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers("/api/admin/admins/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/admin/users/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/admin/baristas/**").hasRole("ADMIN")
                        // Baristas need read-only access to the catalog to ring up sales, and to
                        // stock levels/movements/low-stock so they can see what an order deducted
                        // and flag what needs restocking — without being able to adjust it
                        // themselves (stock-in/stock-cut stay Admin-only, matched below).
                        .requestMatchers(HttpMethod.GET, "/api/admin/categories/**", "/api/admin/products/**",
                                "/api/admin/inventory/**", "/api/admin/extras/**")
                        .hasAnyRole("ADMIN", "BARISTA")
                        .requestMatchers("/api/admin/categories/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/products/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/inventory/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/extras/**").hasRole("ADMIN")
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
                        .requestMatchers("/api/customer/**").hasRole("CUSTOMER")
                        // Deny-by-default backstop: every actual admin/barista endpoint above is
                        // matched by an explicit role rule, so anything still reaching this point
                        // is either a mistyped path or a new endpoint someone forgot to add a rule
                        // for — either way it must never fall through to the generic
                        // anyRequest().authenticated() below, which would let ANY authenticated
                        // role (including a customer) reach it just for being logged in.
                        .requestMatchers("/api/admin/**", "/api/barista/**").denyAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}