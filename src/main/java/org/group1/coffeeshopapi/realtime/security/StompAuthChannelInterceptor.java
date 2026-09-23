package org.group1.coffeeshopapi.realtime.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.constant.RedisKeys;
import org.group1.coffeeshopapi.common.constant.SecurityConstants;
import org.group1.coffeeshopapi.common.security.CustomUserDetailsService;
import org.group1.coffeeshopapi.common.util.JwtUtil;
import org.group1.coffeeshopapi.realtime.RealtimeDestinations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Set;

// Authenticates the STOMP CONNECT frame with the same JWT rules as JwtAuthFilter, and checks
// who may subscribe to what.
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Set<String> STAFF_ROLES = Set.of("ROLE_ADMIN", "ROLE_BARISTA", "ROLE_SUPER_ADMIN");

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final StringRedisTemplate redisTemplate;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        StompCommand command = accessor.getCommand();
        if (command == StompCommand.CONNECT) {
            accessor.setUser(authenticate(accessor.getFirstNativeHeader(SecurityConstants.JWT_HEADER)));
        } else if (command == StompCommand.SUBSCRIBE) {
            authorizeSubscribe(accessor.getUser(), accessor.getDestination());
        } else if (command == StompCommand.SEND) {
            // Nothing is handled on /app yet — the socket is push-only.
            throw new MessageDeliveryException("Sending messages is not supported");
        }
        return message;
    }

    private Authentication authenticate(String header) {
        if (header == null || !header.startsWith(SecurityConstants.JWT_PREFIX)) {
            throw new MessageDeliveryException("Missing bearer token");
        }
        try {
            Claims claims = jwtUtil.extractClaims(header.substring(SecurityConstants.JWT_PREFIX.length()));
            if (!jwtUtil.isAccessToken(claims)
                    || Boolean.TRUE.equals(redisTemplate.hasKey(RedisKeys.JWT_DENYLIST_PREFIX + claims.getId()))) {
                throw new MessageDeliveryException("Invalid token");
            }
            UserDetails userDetails = userDetailsService.loadUserByUsername(claims.getSubject());
            if (!userDetails.isEnabled()) {
                throw new MessageDeliveryException("Account is not active");
            }
            return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException ex) {
            throw new MessageDeliveryException("Invalid token");
        }
    }

    private void authorizeSubscribe(Principal user, String destination) {
        if (!(user instanceof Authentication auth) || !auth.isAuthenticated()) {
            throw new MessageDeliveryException("Not authenticated");
        }
        if (destination == null) {
            throw new MessageDeliveryException("Missing destination");
        }
        // Spring resolves /user/** to this session's own queue, so any signed-in user may use it.
        if (destination.startsWith("/user/")) {
            return;
        }
        if (destination.startsWith(RealtimeDestinations.STAFF_ORDERS) && isStaff(auth)) {
            return;
        }
        throw new MessageDeliveryException("Not allowed to subscribe to " + destination);
    }

    private boolean isStaff(Authentication auth) {
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if (STAFF_ROLES.contains(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
