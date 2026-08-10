package org.group1.coffeeshopapi.common.config;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// Without this, every @PreAuthorize in the app (including this class's own rules below) is silently ignored.
@EnableMethodSecurity
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/register",
                                "/api/auth/verify-otp",
                                "/api/auth/resend-otp",
                                "/api/auth/login",
                                "/api/auth/refresh-token",
                                "/api/auth/forgot-password",
                                "/api/auth/verify-reset-otp",
                                "/api/auth/reset-password",
                                "/api/v1/auth/register",
                                "/api/v1/auth/verify-otp",
                                "/api/v1/auth/resend-otp",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh-token",
                                "/api/v1/auth/forgot-password",
                                "/api/v1/auth/verify-reset-otp",
                                "/api/v1/auth/reset-password"
                        )
                        .permitAll()
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/v1/api-docs/**",
                                "/v1/api-docs"
                        )
                        .permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/categories/**",
                                "/api/v1/product/**",
                                "/api/v1/files/**",
                                "/api/v1/events/**"
                        )
                        .permitAll()
                        .requestMatchers("/api/v1/admin/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/api/v1/barista/**")
                        .hasAnyRole("ADMIN", "BARISTA")
                        .requestMatchers("/api/v1/kitchen/**")
                        .hasAnyRole("ADMIN", "BARISTA")
                        .requestMatchers("/api/v1/customer/**")
                        .hasAnyRole("ADMIN", "CUSTOMER")
                        .anyRequest()
                        .authenticated()
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(
                        jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class
                );
        return http.build();
    }
}
