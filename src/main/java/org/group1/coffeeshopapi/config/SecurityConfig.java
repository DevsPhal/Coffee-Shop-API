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

    // Lets a browser-based frontend on another origin actually call this API.
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
                        // Baristas get read-only access to the catalog and stock levels, but can't
                        // change stock themselves — that stays Admin-only, matched below.
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
                        // Deny-by-default backstop for any admin/barista path not explicitly
                        // matched above, so it can't fall through to anyRequest().authenticated().
                        .requestMatchers("/api/admin/**", "/api/barista/**").denyAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}