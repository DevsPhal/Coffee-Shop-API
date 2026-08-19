package org.group1.coffeeshopapi.common.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.constant.RedisKeys;
import org.group1.coffeeshopapi.common.constant.SecurityConstants;
import org.group1.coffeeshopapi.common.security.CustomUserDetailsService;
import org.group1.coffeeshopapi.common.util.JwtUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(SecurityConstants.JWT_HEADER);
        if (header != null && header.startsWith(SecurityConstants.JWT_PREFIX)) {
            String token = header.substring(SecurityConstants.JWT_PREFIX.length());
            try {
                Claims claims = jwtUtil.extractClaims(token);
                boolean denylisted = redisTemplate.hasKey(RedisKeys.JWT_DENYLIST_PREFIX + claims.getId());
                if (jwtUtil.isAccessToken(claims) && !denylisted
                        && SecurityContextHolder.getContext().getAuthentication() == null) {
                    String email = claims.getSubject();
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                    // Re-checked on every request (not just at login) so a deactivated account's
                    // still-valid access token stops working immediately, not just at its natural expiry.
                    if (userDetails.isEnabled()) {
                        var auth = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (JwtException | IllegalArgumentException | UsernameNotFoundException ignored) {
                // Invalid/expired token, or the account no longer exists (e.g. deleted) — treat as unauthenticated.
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}