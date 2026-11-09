package org.group1.coffeeshopapi.common.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group1.coffeeshopapi.common.constant.RedisKeys;
import org.group1.coffeeshopapi.common.constant.SecurityConstants;
import org.group1.coffeeshopapi.common.response.ErrorResponse;
import org.group1.coffeeshopapi.common.security.CustomUserDetailsService;
import org.group1.coffeeshopapi.common.util.JwtUtil;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

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

                    if (userDetails.isEnabled()) {
                        var auth = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (JwtException | IllegalArgumentException | UsernameNotFoundException ignored) {
                SecurityContextHolder.clearContext();
            } catch (DataAccessException e) {
                // Redis (the token denylist) or the database is down: say so, rather than a misleading 401.
                log.error("Could not check the access token on {} {}", request.getMethod(), request.getRequestURI(), e);
                SecurityContextHolder.clearContext();
                writeUnavailable(request, response);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private void writeUnavailable(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .message("The service is temporarily unavailable — please try again in a moment")
                .path(request.getRequestURI())
                .build();
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
